/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.state
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.toggleStateBenchmarkComposeMeasureLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasureLayout
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.ui.integration.test.ToggleableTestCase
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.ui.unit.Constraints
import androidx.ui.test.ComposeTestCase
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class WithConstraintsBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun no_withconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout { NoWithConstraintsTestCase() }
    }

    @Test
    fun withconstraints_inner_recompose() {
        benchmarkRule.toggleStateBenchmarkComposeMeasureLayout { WithConstraintsTestCase() }
    }

    @Test
    fun withconstraints_changing_constraints() {
        benchmarkRule.toggleStateBenchmarkMeasureLayout { ChangingConstraintsTestCase() }
    }
}

private class NoWithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun emitContent() {
        val size = state { 200.dp }
        this.state = size
        Box(Modifier.preferredSize(300.dp), gravity = ContentGravity.Center) {
            Spacer(Modifier.preferredSize(width = size.value, height = size.value))
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class WithConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun emitContent() {
        val size = state { 200.dp }
        this.state = size
        WithConstraints {
            Box(Modifier.preferredSize(300.dp), gravity = ContentGravity.Center) {
                Spacer(Modifier.preferredSize(width = size.value, height = size.value))
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}

private class ChangingConstraintsTestCase : ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Int>

    @Composable
    override fun emitContent() {
        val size = state { 100 }
        this.state = size
        ChangingConstraintsLayout(state) {
            WithConstraints {
                Box(Modifier.fillMaxSize())
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 100) 50 else 100
    }
}

@Composable
private fun ChangingConstraintsLayout(size: State<Int>, children: @Composable () -> Unit) {
    Layout(children) { measurables, _ ->
        val constraints = Constraints.fixed(size.value, size.value)
        with(PlacementScope) { measurables.first().measure(constraints).place(0, 0) }
        layout(100, 100) {}
    }
}

private object PlacementScope : Placeable.PlacementScope() {
    override val parentWidth = 0
    override val parentLayoutDirection = LayoutDirection.Ltr
}
