/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.core

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.Composable
import androidx.compose.Composition
import androidx.compose.CompositionReference
import androidx.compose.ExperimentalComposeApi
import androidx.compose.FrameManager
import androidx.compose.InternalComposeApi
import androidx.compose.Providers
import androidx.compose.Recomposer
import androidx.compose.SlotTable
import androidx.compose.compositionFor
import androidx.compose.currentComposer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.ui.core.selection.SelectionContainer
import androidx.ui.node.UiApplier
import java.util.Collections
import java.util.WeakHashMap

/**
 * Composes the children of the view with the passed in [composable].
 *
 * @see setViewContent
 * @see Composition.dispose
 */
// TODO: Remove this API when View/LayoutNode mixed trees work
@OptIn(ExperimentalComposeApi::class)
fun ViewGroup.setViewContent(
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = compositionFor(
    this,
    UiApplier(this),
    Recomposer.current(),
    parent,
    onCreated = {
        removeAllViews()
    }
).apply {
    setContent {
        Providers(ContextAmbient provides this@setViewContent.context) {
            composable()
        }
    }
}

/**
 * Sets the contentView of an activity to a FrameLayout, and composes the contents of the layout
 * with the passed in [composable].
 *
 * @see setContent
 * @see Activity.setContentView
 */
// TODO: Remove this API when View/LayoutNode mixed trees work
fun Activity.setViewContent(composable: @Composable () -> Unit): Composition {
    // TODO(lmr): add ambients here, or remove API entirely if we can
    // If there is already a FrameLayout in the root, we assume we want to compose
    // into it instead of create a new one. This allows for `setContent` to be
    // called multiple times.
    val root = window
        .decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? ViewGroup
        ?: FrameLayout(this).also { setContentView(it) }
    return root.setViewContent(null, composable)
}

// TODO(chuckj): This is a temporary work-around until subframes exist so that
// nextFrame() inside recompose() doesn't really start a new frame, but a new subframe
// instead.
@MainThread
@OptIn(ExperimentalComposeApi::class)
fun subcomposeInto(
    container: LayoutNode,
    recomposer: Recomposer,
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = compositionFor(
    container,
    UiApplier(container),
    recomposer,
    parent
).apply {
    setContent(composable)
}

@Deprecated(
    "Specify Recomposer explicitly",
    ReplaceWith(
        "subcomposeInto(context, container, Recomposer.current(), parent, composable)",
        "androidx.compose.Recomposer"
    )
)
@MainThread
fun subcomposeInto(
    container: LayoutNode,
    parent: CompositionReference? = null,
    composable: @Composable () -> Unit
): Composition = subcomposeInto(container, Recomposer.current(), parent, composable)

/**
 * Composes the given composable into the given activity. The [content] will become the root view
 * of the given activity.
 *
 * [Composition.dispose] is called automatically when the Activity is destroyed.
 *
 * @param recomposer The [Recomposer] to coordinate scheduling of composition updates
 * @param content A `@Composable` function declaring the UI contents
 */
fun ComponentActivity.setContent(
    // Note: Recomposer.current() is the default here since all Activity view trees are hosted
    // on the main thread.
    recomposer: Recomposer = Recomposer.current(),
    content: @Composable () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView: AndroidOwner = window.decorView
        .findViewById<ViewGroup>(android.R.id.content)
        .getChildAt(0) as? AndroidOwner
        ?: AndroidOwner(this, this).also {
            setContentView(it.view, DefaultLayoutParams)
        }
    return doSetContent(composeView, recomposer, content)
}

/**
 * We want text/image selection to be enabled by default and disabled per widget. Therefore a root
 * level [SelectionContainer] is installed at the root.
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun WrapWithSelectionContainer(noinline content: @Composable () -> Unit) {
    SelectionContainer(children = content)
}

/**
 * Composes the given composable into the given view.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 *
 * @param recomposer The [Recomposer] to coordinate scheduling of composition updates
 * @param content Composable that will be the content of the view.
 */
fun ViewGroup.setContent(
    recomposer: Recomposer,
    content: @Composable () -> Unit
): Composition {
    FrameManager.ensureStarted()
    val composeView =
        if (childCount > 0) {
            getChildAt(0) as? AndroidOwner
        } else {
            removeAllViews(); null
        }
            ?: AndroidOwner(context).also { addView(it.view, DefaultLayoutParams) }
    return doSetContent(composeView, recomposer, content)
}

/**
 * Composes the given composable into the given view.
 *
 * Note that this [ViewGroup] should have an unique id for the saved instance state mechanism to
 * be able to save and restore the values used within the composition. See [View.setId].
 *
 * @param content Composable that will be the content of the view.
 */
@Deprecated(
    "Specify Recomposer explicitly",
    ReplaceWith(
        "setContent(Recomposer.current(), content)",
        "androidx.compose.Recomposer"
    )
)
fun ViewGroup.setContent(
    content: @Composable () -> Unit
): Composition = setContent(Recomposer.current(), content)

private fun doSetContent(
    owner: AndroidOwner,
    recomposer: Recomposer,
    content: @Composable () -> Unit
): Composition {
    if (inspectionWanted(owner)) {
        owner.view.setTag(R.id.inspection_slot_table_set,
                Collections.newSetFromMap(WeakHashMap<SlotTable, Boolean>()))
    }
    @OptIn(ExperimentalComposeApi::class)
    val original = compositionFor(owner.root, UiApplier(owner.root), recomposer)
    val wrapped = owner.view.getTag(R.id.wrapped_composition_tag)
            as? WrappedComposition
        ?: WrappedComposition(owner, original).also {
            owner.view.setTag(R.id.wrapped_composition_tag, it)
        }
    wrapped.setContent(content)
    return wrapped
}

private class WrappedComposition(
    val owner: AndroidOwner,
    val original: Composition
) : Composition, LifecycleEventObserver {

    private var disposed = false
    private var addedToLifecycle: Lifecycle? = null

    override fun setContent(content: @Composable () -> Unit) {
        owner.setOnViewTreeOwnersAvailable {
            if (!disposed) {
                if (addedToLifecycle == null) {
                    val lifecycle = it.lifecycleOwner.lifecycle
                    lifecycle.addObserver(this)
                    addedToLifecycle = lifecycle
                }
                if (owner.savedStateRegistry != null) {
                    original.setContent {
                        @Suppress("UNCHECKED_CAST")
                        (owner.view.getTag(R.id.inspection_slot_table_set) as?
                                MutableSet<SlotTable>)
                            ?.let {
                                val composer = currentComposer
                                @OptIn(InternalComposeApi::class)
                                composer.collectKeySourceInformation()
                                it.add(composer.slotTable)
                            }
                        ProvideAndroidAmbients(owner) {
                            WrapWithSelectionContainer(content)
                        }
                    }
                } else {
                    // TODO(Andrey) unify setOnSavedStateRegistryAvailable and
                    //  whenViewTreeOwnersAvailable so we will postpone until we have everything.
                    //  we should migrate to androidx SavedStateRegistry first
                    // we will postpone the composition until composeView restores the state.
                    owner.setOnSavedStateRegistryAvailable {
                        if (!disposed) setContent(content)
                    }
                }
            }
        }
    }

    override fun dispose() {
        if (!disposed) {
            disposed = true
            owner.view.setTag(R.id.wrapped_composition_tag, null)
            addedToLifecycle?.removeObserver(this)
        }
        original.dispose()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            dispose()
        }
    }
}

private val DefaultLayoutParams = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.WRAP_CONTENT,
    ViewGroup.LayoutParams.WRAP_CONTENT
)

/**
 * Determines if inspection is wanted for the Layout Inspector.
 *
 * When DEBUG_VIEW_ATTRIBUTES an/or DEBUG_VIEW_ATTRIBUTES_APPLICATION_PACKAGE is turned on for the
 * current application the Layout Inspector is inspecting. An application cannot directly access
 * these global settings, nor is the static field: View.sDebugViewAttributes available.
 *
 *
 * Instead check if the attributeSourceResourceMap is not empty.
 */
private fun inspectionWanted(owner: AndroidOwner): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            owner.view.attributeSourceResourceMap.isNotEmpty()
