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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.FirstBaseline
import androidx.ui.core.IntPx
import androidx.ui.core.LastBaseline
import androidx.ui.core.LayoutCoordinates
import androidx.ui.core.OnChildPositioned
import androidx.ui.core.PxPosition
import androidx.ui.core.Text
import androidx.ui.core.dp
import androidx.ui.core.globalPosition
import androidx.ui.core.round
import androidx.ui.core.withDensity
import androidx.ui.layout.DpConstraints
import androidx.ui.layout.Wrap
import androidx.ui.test.assertIsVisible
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByText
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@MediumTest
@RunWith(JUnit4::class)
class SnackbarTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    val longText = "Message Is very long and long and long and long and long " +
            "and long and long and long and long and long and long"

    @Test
    fun defaultSnackbar_semantics() {
        var clicked = false
        composeTestRule.setMaterialContent {
            Wrap {
                Snackbar(text = "Message", actionText = "UNDO", onActionClick = { clicked = true })
            }
        }

        findByText("Message")
            .assertIsVisible()

        assertThat(clicked).isFalse()

        findByText("UNDO")
            .assertIsVisible()
            .doClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun snackbar_shortTextOnly_sizes() {
        var textCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    OnChildPositioned(onPositioned = { textCoords = it }) {
                        Text("Message")
                    }
                }
            )
        }
        sizes
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        textCoords?.let {
            withDensity(composeTestRule.density) {
                val alignmentLines = it.providedAlignmentLines
                assertThat(alignmentLines.get(FirstBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(alignmentLines.get(FirstBaseline))
                    .isEqualTo(alignmentLines.get(LastBaseline))
                assertThat(it.position.y.round() + alignmentLines.getValue(FirstBaseline))
                    .isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_shortTextAndButton_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        var buttonTextCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text("Message")
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            Button(style = TextButtonStyle(), onClick = {}) {
                                OnChildPositioned(onPositioned = { buttonTextCoords = it }) {
                                    Text("Undo")
                                }
                            }
                        }
                    }
                )
            }
        }
        sizes
            .assertHeightEqualsTo(48.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(buttonTextCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localButtonTextCoords = buttonTextCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null &&
            localButtonCoords != null &&
            localButtonTextCoords != null &&
            localSnackCoords != null
        ) {
            withDensity(composeTestRule.density) {
                val textAlignmentLines = localTextCoords.providedAlignmentLines
                val buttonAlignmentLines = localButtonTextCoords.providedAlignmentLines
                val buttonTextPos =
                    localSnackCoords.childToLocal(localButtonTextCoords, PxPosition.Origin)
                assertThat(textAlignmentLines.get(FirstBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(buttonAlignmentLines.get(FirstBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(
                    localTextCoords.globalPosition.y.round() +
                            textAlignmentLines.getValue(FirstBaseline)
                ).isEqualTo(30.dp.toIntPx())
                assertThat(
                    buttonTextPos.y.round() + buttonAlignmentLines.getValue(FirstBaseline)
                ).isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_longText_sizes() {
        var textCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            Snackbar(
                text = {
                    OnChildPositioned(onPositioned = { textCoords = it }) {
                        Text(longText, maxLines = 2)
                    }
                }
            )
        }
        sizes
            .assertHeightEqualsTo(68.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        textCoords?.let {
            withDensity(composeTestRule.density) {
                val alignmentLines = it.providedAlignmentLines

                assertThat(alignmentLines.get(FirstBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(alignmentLines.get(LastBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(alignmentLines.get(FirstBaseline))
                    .isNotEqualTo(alignmentLines.get(LastBaseline))
                assertThat(it.position.y.round() + alignmentLines.getValue(FirstBaseline))
                    .isEqualTo(30.dp.toIntPx())
            }
        }
    }

    @Test
    fun snackbar_longTextAndButton_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        val sizes = composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text(longText, maxLines = 2)
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            Button(text = "Undo", style = TextButtonStyle(), onClick = {})
                        }
                    }
                )
            }
        }
        sizes
            .assertHeightEqualsTo(68.dp)
            .assertWidthEqualsTo(300.dp)
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null && localButtonCoords != null && localSnackCoords != null) {
            withDensity(composeTestRule.density) {
                val textAlignmentLines = localTextCoords.providedAlignmentLines
                val buttonPositionInSnack =
                    localSnackCoords.childToLocal(localButtonCoords, PxPosition.Origin)
                val buttonCenter =
                    buttonPositionInSnack.y + localButtonCoords.size.height / 2

                assertThat(textAlignmentLines.get(FirstBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(textAlignmentLines.get(LastBaseline)).isNotEqualTo(IntPx.Zero)
                assertThat(textAlignmentLines.get(FirstBaseline))
                    .isNotEqualTo(textAlignmentLines.get(LastBaseline))
                assertThat(
                    localTextCoords.globalPosition.y.round() +
                            textAlignmentLines.getValue(FirstBaseline)
                ).isEqualTo(30.dp.toIntPx())

                assertThat(buttonCenter).isEqualTo(localSnackCoords.size.height / 2)
            }
        }
    }

    @Test
    fun snackbar_textAndButtonOnSeparateLine_alignment() {
        var snackCoords: LayoutCoordinates? = null
        var textCoords: LayoutCoordinates? = null
        var buttonCoords: LayoutCoordinates? = null
        composeTestRule.setMaterialContentAndCollectSizes(
            parentConstraints = DpConstraints(maxWidth = 300.dp)
        ) {
            OnChildPositioned(onPositioned = { snackCoords = it }) {
                Snackbar(
                    text = {
                        OnChildPositioned(onPositioned = { textCoords = it }) {
                            Text("Message")
                        }
                    },
                    action = {
                        OnChildPositioned(onPositioned = { buttonCoords = it }) {
                            Button(text = "Undo", style = TextButtonStyle(), onClick = {})
                        }
                    },
                    actionOnNewLine = true
                )
            }
        }
        assertThat(textCoords).isNotNull()
        assertThat(buttonCoords).isNotNull()
        assertThat(snackCoords).isNotNull()
        val localTextCoords = textCoords
        val localButtonCoords = buttonCoords
        val localSnackCoords = snackCoords

        if (localTextCoords != null && localButtonCoords != null && localSnackCoords != null) {
            withDensity(composeTestRule.density) {
                val textAlignmentLines = localTextCoords.providedAlignmentLines
                val buttonPositionInSnack =
                    localSnackCoords.childToLocal(localButtonCoords, PxPosition.Origin)
                val textPositionInSnack =
                    localSnackCoords.childToLocal(localTextCoords, PxPosition.Origin)

                assertThat(
                    textPositionInSnack.y.round() + textAlignmentLines.getValue(FirstBaseline)
                ).isEqualTo(30.dp.toIntPx())

                assertThat(
                    buttonPositionInSnack.y.round() - textPositionInSnack.y.round() -
                            textAlignmentLines.getValue(LastBaseline)
                ).isEqualTo(18.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.height.round() - buttonPositionInSnack.y.round() -
                            localButtonCoords.size.height.round()
                ).isEqualTo(8.dp.toIntPx())

                assertThat(
                    localSnackCoords.size.width.round() - buttonPositionInSnack.x.round() -
                            localButtonCoords.size.width.round()
                ).isEqualTo(8.dp.toIntPx())
            }
        }
    }
}