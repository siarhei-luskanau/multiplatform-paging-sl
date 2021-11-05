/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.appwidget.demos

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.ActionRunnable
import androidx.glance.action.actionUpdateContent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Button
import androidx.glance.layout.Column
import androidx.glance.layout.Image
import androidx.glance.layout.ImageProvider
import androidx.glance.layout.Text
import androidx.glance.layout.Visibility
import androidx.glance.layout.padding
import androidx.glance.layout.visibility
import androidx.glance.text.FontWeight
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle

class UpdatingAppWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    @Composable
    override fun Content() {
        Column(modifier = GlanceModifier.padding(8.dp)) {
            Text(
                LocalContext.current.getString(R.string.updating_title),
                style = TextStyle(
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            )
            Button(
                if (showImage) "Hide Image" else "Show Image",
                onClick = actionUpdateContent<UpdateTitleAction>()
            )
            Box(
                modifier = GlanceModifier.visibility(
                    if (showImage) Visibility.Visible else Visibility.Gone
                ),
                contentAlignment = Alignment.Center
            ) {
                Image(ImageProvider(R.drawable.compose), "Compose logo")
            }
        }
    }
}

class UpdatingAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpdatingAppWidget()
}

private var showImage: Boolean = false

class UpdateTitleAction : ActionRunnable {
    override suspend fun run(context: Context, parameters: ActionParameters) {
        showImage = !showImage
    }
}
