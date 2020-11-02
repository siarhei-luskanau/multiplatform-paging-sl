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

package androidx.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.HorizontalGradient
import androidx.compose.ui.unit.dp

@Composable
@Sampled
fun DrawBackgroundColor() {
    Text(
        "Text with background",
        Modifier.background(color = Color.Magenta).padding(10.dp)
    )
}

@Composable
@Sampled
fun DrawBackgroundShapedBrush() {
    val gradientBrush = HorizontalGradient(
        colors = listOf(Color.Red, Color.Blue, Color.Green),
        startX = 0.0f,
        endX = 500.0f
    )
    Text(
        "Text with gradient back",
        Modifier.background(brush = gradientBrush, shape = CutCornerShape(8.dp))
            .padding(10.dp)
    )
}