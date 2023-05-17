/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.watchface.complications.data

import android.icu.util.ULocale
import android.support.wearable.complications.ComplicationData as WireComplicationData
import android.support.wearable.complications.ComplicationData.Companion.TYPE_NO_DATA
import android.support.wearable.complications.ComplicationText as WireComplicationText
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicFloat
import androidx.wear.protolayout.expression.PlatformDataKey
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.pipeline.BoundDynamicType
import androidx.wear.protolayout.expression.pipeline.DynamicTypeBindingRequest
import androidx.wear.protolayout.expression.pipeline.DynamicTypeEvaluator
import androidx.wear.protolayout.expression.pipeline.DynamicTypeValueReceiver
import androidx.wear.protolayout.expression.pipeline.SensorGatewaySingleDataProvider
import androidx.wear.protolayout.expression.pipeline.StateStore
import androidx.wear.protolayout.expression.pipeline.TimeGateway
import androidx.wear.protolayout.expression.pipeline.sensor.SensorGateway
import java.util.Collections
import java.util.concurrent.Executor
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

/**
 * Evaluates a [WireComplicationData] with
 * [androidx.wear.protolayout.expression.DynamicBuilders.DynamicType] within its fields.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class DynamicComplicationDataEvaluator(
    private val stateStore: StateStore? = StateStore(emptyMap()),
    private val timeGateway: TimeGateway? = null,
    // TODO(b/281664278): remove the SensorGateway usage, implement PlatformDataProvider instead.
    private val sensorGateway: SensorGateway? = null,
    private val keepDynamicValues: Boolean = false,
) {
    /**
     * Returns a [Flow] that provides the evaluated [WireComplicationData].
     *
     * The dynamic values are evaluated _separately_ on each flow collection.
     */
    fun evaluate(unevaluatedData: WireComplicationData): Flow<WireComplicationData> =
        evaluateTopLevelFields(unevaluatedData)
            // Combining with fields that are made of WireComplicationData.
            .combineWithDataList(unevaluatedData.timelineEntries) { entries ->
                // Timeline entries are set on the built WireComplicationData.
                WireComplicationData.Builder(
                    this@combineWithDataList.build().apply { setTimelineEntryCollection(entries) }
                )
            }
            .combineWithDataList(unevaluatedData.listEntries) { setListEntryCollection(it) }
            // Must be last, as it overwrites INVALID_DATA.
            .combineWithEvaluatedPlaceholder(unevaluatedData.placeholder)
            .distinctUntilChanged()

    /** Evaluates "local" fields, excluding fields of type WireComplicationData. */
    private fun evaluateTopLevelFields(
        unevaluatedData: WireComplicationData
    ): Flow<WireComplicationData> = flow {
        val state: MutableStateFlow<State> = unevaluatedData.buildState()
        state.value.use {
            val evaluatedData: Flow<WireComplicationData> =
                state.mapNotNull {
                    when {
                        // Emitting INVALID_DATA if there's an invalid receiver.
                        it.invalidReceivers.isNotEmpty() -> INVALID_DATA
                        // Emitting the data if all pending receivers are done and all
                        // pre-updates are satisfied.
                        it.pendingReceivers.isEmpty() -> it.data
                        // Skipping states that are not ready for be emitted.
                        else -> null
                    }
                }
            emitAll(evaluatedData)
        }
    }

    /**
     * Combines the receiver with the evaluated version of the provided list.
     *
     * If the receiver [Flow] emits [INVALID_DATA] or the input list is null or empty, this does not
     * mutate the flow and does not wait for the entries to finish evaluating.
     *
     * If even one [WireComplicationData] within the provided list is evaluated to [INVALID_DATA],
     * the output [Flow] becomes [INVALID_DATA] (the receiver [Flow] is ignored).
     */
    private fun Flow<WireComplicationData>.combineWithDataList(
        unevaluatedEntries: List<WireComplicationData>?,
        setter:
            WireComplicationData.Builder.(
                List<WireComplicationData>
            ) -> WireComplicationData.Builder,
    ): Flow<WireComplicationData> {
        if (unevaluatedEntries.isNullOrEmpty()) return this
        val evaluatedEntriesFlow: Flow<List<WireComplicationData>> =
            combine(unevaluatedEntries.map { evaluate(it) })

        return this.combine(evaluatedEntriesFlow).map {
            (data: WireComplicationData, evaluatedEntries: List<WireComplicationData>?) ->
            // Not mutating if invalid.
            if (data === INVALID_DATA) return@map data
            // An entry is invalid, emitting invalid.
            if (evaluatedEntries.any { it === INVALID_DATA }) return@map INVALID_DATA
            // All is well, mutating the input.
            return@map WireComplicationData.Builder(data).setter(evaluatedEntries).build()
        }
    }

    /**
     * Same as [combineWithDataList], but sets the evaluated placeholder ONLY when the receiver
     * [Flow] emits [TYPE_NO_DATA], or [keepDynamicValues] is true, otherwise clears it and does not
     * wait for the placeholder to finish evaluating.
     *
     * If the placeholder is not required (per the above paragraph), this doesn't wait for it.
     */
    private fun Flow<WireComplicationData>.combineWithEvaluatedPlaceholder(
        unevaluatedPlaceholder: WireComplicationData?
    ): Flow<WireComplicationData> {
        if (unevaluatedPlaceholder == null) return this
        val evaluatedPlaceholderFlow: Flow<WireComplicationData> = evaluate(unevaluatedPlaceholder)

        return this.combine(evaluatedPlaceholderFlow).map {
            (data: WireComplicationData, evaluatedPlaceholder: WireComplicationData?) ->
            if (!keepDynamicValues && data.type != TYPE_NO_DATA) {
                // Clearing the placeholder when data is not TYPE_NO_DATA (it was meant as an
                // dynamic value fallback).
                return@map WireComplicationData.Builder(data).setPlaceholder(null).build()
            }
            // Placeholder required but invalid, emitting invalid.
            if (evaluatedPlaceholder === INVALID_DATA) return@map INVALID_DATA
            // All is well, mutating the input.
            return@map WireComplicationData.Builder(data)
                .setPlaceholder(evaluatedPlaceholder)
                .build()
        }
    }

    private suspend fun WireComplicationData.buildState() =
        MutableStateFlow(State(this)).apply {
            if (hasRangedDynamicValue()) {
                addReceiver(
                    rangedDynamicValue,
                    dynamicValueTrimmer = { setRangedDynamicValue(null) },
                    setter = { setRangedValue(it) },
                )
            }
            if (hasLongText()) addReceiver(longText) { setLongText(it) }
            if (hasLongTitle()) addReceiver(longTitle) { setLongTitle(it) }
            if (hasShortText()) addReceiver(shortText) { setShortText(it) }
            if (hasShortTitle()) addReceiver(shortTitle) { setShortTitle(it) }
            if (hasContentDescription()) {
                addReceiver(contentDescription) { setContentDescription(it) }
            }
            // Add all the receivers before we start binding them because binding can synchronously
            // trigger the receiver, which would update the data before all the fields are
            // evaluated.
            value.initEvaluation()
        }

    private suspend fun MutableStateFlow<State>.addReceiver(
        dynamicValue: DynamicFloat?,
        dynamicValueTrimmer: WireComplicationData.Builder.() -> WireComplicationData.Builder,
        setter: WireComplicationData.Builder.(Float) -> WireComplicationData.Builder,
    ) {
        dynamicValue ?: return
        val executor = currentCoroutineContext().asExecutor()
        update { state ->
            state.withPendingReceiver(
                ComplicationEvaluationResultReceiver<Float>(
                    this,
                    setter = { value ->
                        if (!keepDynamicValues) dynamicValueTrimmer(this)
                        setter(this, value)
                    },
                    binder = { receiver ->
                        value.evaluator.bind(
                            DynamicTypeBindingRequest.forDynamicFloat(
                                dynamicValue,
                                executor,
                                receiver
                            )
                        )
                    },
                )
            )
        }
    }

    private suspend fun MutableStateFlow<State>.addReceiver(
        text: WireComplicationText?,
        setter: WireComplicationData.Builder.(WireComplicationText) -> WireComplicationData.Builder,
    ) {
        val dynamicValue = text?.dynamicValue ?: return
        val executor = currentCoroutineContext().asExecutor()
        update {
            it.withPendingReceiver(
                ComplicationEvaluationResultReceiver<String>(
                    this,
                    setter = { value ->
                        setter(
                            if (keepDynamicValues) {
                                WireComplicationText(value, dynamicValue)
                            } else {
                                WireComplicationText(value)
                            }
                        )
                    },
                    binder = { receiver ->
                        value.evaluator.bind(
                            DynamicTypeBindingRequest.forDynamicString(
                                dynamicValue,
                                ULocale.getDefault(),
                                executor,
                                receiver
                            )
                        )
                    },
                )
            )
        }
    }

    /**
     * Holds the state of the continuously evaluated [WireComplicationData] and the various
     * [ComplicationEvaluationResultReceiver] that are evaluating it.
     */
    private inner class State(
        val data: WireComplicationData,
        val pendingReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val invalidReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
        val completeReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> = setOf(),
    ) : AutoCloseable {
        lateinit var evaluator: DynamicTypeEvaluator

        fun withPendingReceiver(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            copy(pendingReceivers = pendingReceivers + receiver)

        fun withInvalidReceiver(receiver: ComplicationEvaluationResultReceiver<out Any>) =
            copy(
                pendingReceivers = pendingReceivers - receiver,
                invalidReceivers = invalidReceivers + receiver,
                completeReceivers = completeReceivers - receiver,
            )

        fun withUpdatedData(
            data: WireComplicationData,
            receiver: ComplicationEvaluationResultReceiver<out Any>,
        ) =
            copy(
                data,
                pendingReceivers = pendingReceivers - receiver,
                invalidReceivers = invalidReceivers - receiver,
                completeReceivers = completeReceivers + receiver,
            )

        /**
         * Initializes the internal [DynamicTypeEvaluator] if there are pending receivers.
         *
         * Should be called after all receivers were added.
         */
        suspend fun initEvaluation() {
            if (pendingReceivers.isEmpty()) return
            require(!this::evaluator.isInitialized) { "initEvaluator must be called exactly once." }
            evaluator =
                DynamicTypeEvaluator(
                    DynamicTypeEvaluator.Config.Builder()
                        .apply { stateStore?.let { setStateStore(it) } }
                        .apply { timeGateway?.let { setTimeGateway(it) } }
                        .apply { sensorGateway?.let {
                            addPlatformDataProvider(
                                SensorGatewaySingleDataProvider(
                                    sensorGateway, PlatformHealthSources.Keys.HEART_RATE_BPM
                                ),
                                Collections.singleton(PlatformHealthSources.Keys.HEART_RATE_BPM)
                                    as Set<PlatformDataKey<*>>
                            )
                            addPlatformDataProvider(
                                SensorGatewaySingleDataProvider(
                                    sensorGateway, PlatformHealthSources.Keys.DAILY_STEPS
                                ),
                                Collections.singleton(PlatformHealthSources.Keys.DAILY_STEPS)
                                    as Set<PlatformDataKey<*>>
                            )
                        } }
                        .build()
                )
            try {
                for (receiver in pendingReceivers) receiver.bind()
                // TODO(b/270697243): Remove this invoke once DynamicTypeEvaluator is thread safe.
                Dispatchers.Main.immediate.invoke {
                    // These need to be called on the main thread.
                    for (receiver in pendingReceivers) receiver.startEvaluation()
                }
            } catch (e: Throwable) {
                // Cleanup on initialization failure.
                close()
                throw e
            }
        }

        override fun close() {
            // TODO(b/270697243): Remove this launch once DynamicTypeEvaluator is thread safe.
            CoroutineScope(Dispatchers.Main.immediate).launch {
                // These need to be called on the main thread.
                for (receiver in pendingReceivers + invalidReceivers + completeReceivers) {
                    receiver.close()
                }
            }
        }

        private fun copy(
            data: WireComplicationData = this.data,
            pendingReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.pendingReceivers,
            invalidReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.invalidReceivers,
            completeReceivers: Set<ComplicationEvaluationResultReceiver<out Any>> =
                this.completeReceivers,
        ) =
            State(
                data = data,
                pendingReceivers = pendingReceivers,
                invalidReceivers = invalidReceivers,
                completeReceivers = completeReceivers,
            )
    }

    private inner class ComplicationEvaluationResultReceiver<T : Any>(
        private val state: MutableStateFlow<State>,
        private val setter: WireComplicationData.Builder.(T) -> WireComplicationData.Builder,
        private val binder: (ComplicationEvaluationResultReceiver<T>) -> BoundDynamicType,
    ) : DynamicTypeValueReceiver<T>, AutoCloseable {
        @Volatile // In case bind() and close() are called on different threads.
        private lateinit var boundDynamicType: BoundDynamicType

        fun bind() {
            boundDynamicType = binder(this)
        }

        // TODO(b/270697243): Remove this annotation once DynamicTypeEvaluator is thread safe.
        @MainThread
        fun startEvaluation() {
            boundDynamicType.startEvaluation()
        }

        // TODO(b/270697243): Remove this annotation once DynamicTypeEvaluator is thread safe.
        @MainThread
        override fun close() {
            boundDynamicType.close()
        }

        override fun onData(newData: T) {
            state.update {
                it.withUpdatedData(
                    setter(WireComplicationData.Builder(it.data), newData).build(),
                    this
                )
            }
        }

        override fun onInvalidated() {
            state.update { it.withInvalidReceiver(this) }
        }
    }

    companion object {
        val INVALID_DATA: WireComplicationData = NoDataComplicationData().asWireComplicationData()
    }
}

/**
 * Replacement for CoroutineDispatcher.asExecutor extension due to
 * https://github.com/Kotlin/kotlinx.coroutines/pull/3683.
 */
internal fun CoroutineContext.asExecutor() = Executor { runnable ->
    val dispatcher = this[ContinuationInterceptor] as CoroutineDispatcher
    if (dispatcher.isDispatchNeeded(this)) {
        dispatcher.dispatch(this, runnable)
    } else {
        runnable.run()
    }
}

/** Replacement of [kotlinx.coroutines.flow.combine], which doesn't seem to work. */
internal fun <T> combine(flows: List<Flow<T>>): Flow<List<T>> = flow {
    data class ValueExists(val value: T?, val exists: Boolean)
    val latest = MutableStateFlow(List(flows.size) { ValueExists(null, false) })
    @Suppress("UNCHECKED_CAST") // Flow<List<T?>> -> Flow<List<T>> safe after filtering exists.
    emitAll(
        flows
            .mapIndexed { i, flow -> flow.map { i to it } } // List<Flow<Int, T>> (indexed flows)
            .merge() // Flow<Int, T>
            .map { (i, value) ->
                // Updating latest and returning the current latest.
                latest.updateAndGet {
                    val newLatest = it.toMutableList()
                    newLatest[i] = ValueExists(value, true)
                    newLatest
                }
            } // Flow<List<ValueExists>>
            // Filtering emissions until we have all values.
            .filter { values -> values.all { it.exists } }
            // Flow<List<T>> + defensive copy.
            .map { values -> values.map { it.value } } as Flow<List<T>>
    )
}

/**
 * Another replacement of [kotlinx.coroutines.flow.combine] which is similar to
 * `combine(List<Flow<T>>)` but allows different types for each flow.
 */
@Suppress("UNCHECKED_CAST")
internal fun <T1, T2> Flow<T1>.combine(other: Flow<T2>): Flow<Pair<T1, T2>> =
    combine(listOf(this as Flow<*>, other as Flow<*>)).map { (a, b) -> (a as T1) to (b as T2) }
