/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.state
import androidx.compose.foundation.ClickableText
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.Text
import androidx.compose.ui.text.AnnotatedString

@Composable
fun InteractiveTextDemo() {
    TextOnClick()
}

@Composable
fun TextOnClick() {
    val clickedOffset = state { -1 }
    ScrollableColumn {
        Text("Clicked Offset: ${clickedOffset.value}")
        ClickableText(
            text = AnnotatedString("Click Me")
        ) { offset ->
            clickedOffset.value = offset
        }
    }
}