/*
 * Copyright 2024 The Android Open Source Project
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
package androidx.glance.appwidget.components

import android.os.Build
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonColors
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.NoRippleOverride
import androidx.glance.action.action
import androidx.glance.action.clickable
import androidx.glance.appwidget.R
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.enabled
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * A button styled per Material3. It has a filled background. It is more opinionated than [Button]
 * and suitable for uses where M3 is preferred.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param icon An optional leading icon placed before the text.
 * @param colors The colors to use for the background and content of the button.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 * @param key A stable and unique key that identifies the action for this button. This ensures
 * that the correct action is triggered, especially in cases of items that change order. If not
 * provided we use the key that is automatically generated by the Compose runtime, which is unique
 * for every exact code location in the composition tree.
 */
@Composable
fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    icon: ImageProvider? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    maxLines: Int = Int.MAX_VALUE,
    key: String? = null
) = FilledButton(
    text = text,
    onClick = action(block = onClick, key = key),
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    colors = colors,
    maxLines = maxLines,
)

/**
 * A button styled per Material3. It has a filled background. It is more opinionated than [Button]
 * and suitable for uses where M3 is preferred.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param icon An optional leading icon placed before the text.
 * @param colors The colors to use for the background and content of the button.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun FilledButton(
    text: String,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    icon: ImageProvider? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    maxLines: Int = Int.MAX_VALUE,
) = M3TextButton(
    text = text,
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    contentColor = colors.contentColor,
    backgroundTint = colors.backgroundColor,
    backgroundResource = R.drawable.glance_component_btn_filled,
    onClick = onClick,
    maxLines = maxLines,
)

/**
 * An outline button styled per Material3. It has a transparent background. It is more opinionated
 * than [Button] and suitable for uses where M3 is preferred.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param icon An optional leading icon placed before the text.
 * @param contentColor The color used for the text, optional icon tint, and outline.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 * @param key A stable and unique key that identifies the action for this button. This ensures
 * that the correct action is triggered, especially in cases of items that change order. If not
 * provided we use the key that is automatically generated by the Compose runtime, which is unique
 * for every exact code location in the composition tree.
 */
@Composable
fun OutlineButton(
    text: String,
    contentColor: ColorProvider,
    onClick: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    icon: ImageProvider? = null,
    maxLines: Int = Int.MAX_VALUE,
    key: String? = null
) = OutlineButton(
    text = text,
    contentColor = contentColor,
    onClick = action(block = onClick, key = key),
    modifier = modifier,
    enabled = enabled,
    icon = icon,
    maxLines = maxLines,
)

/**
 * An outline button styled per Material3. It has a transparent background. It is more opinionated
 * than [Button] and suitable for uses where M3 is preferred.
 *
 * @param text The text that this button will show.
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param icon An optional leading icon placed before the text.
 * @param contentColor The color used for the text, optional icon tint, and outline.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated.
 */
@Composable
fun OutlineButton(
    text: String,
    contentColor: ColorProvider,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    icon: ImageProvider? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val bg: ColorProvider = contentColor
    val fg: ColorProvider = contentColor

    M3TextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        icon = icon,
        contentColor = fg,
        backgroundResource = R.drawable.glance_component_btn_outline,
        backgroundTint = bg,
        maxLines = maxLines,
    )
}

/**
 * Intended to fill the role of primary icon button or fab.
 *
 * @param imageProvider the icon to be drawn in the button
 * @param contentDescription Text used by accessibility services to describe what this image
 * represents. This text should be localized, such as by using
 * androidx.compose.ui.res.stringResource or similar
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param backgroundColor The color to tint the button's background.
 * @param contentColor The color to tint the button's icon.
 * @param key A stable and unique key that identifies the action for this button. This ensures
 * that the correct action is triggered, especially in cases of items that change order. If not
 * provided we use the key that is automatically generated by the Compose runtime, which is unique
 * for every exact code location in the composition tree.
 */
@Composable
fun SquareIconButton(
    imageProvider: ImageProvider,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    backgroundColor: ColorProvider = GlanceTheme.colors.primary,
    contentColor: ColorProvider = GlanceTheme.colors.onPrimary,
    key: String? = null
) = SquareIconButton(
    imageProvider = imageProvider,
    contentDescription = contentDescription,
    onClick = action(block = onClick, key = key),
    modifier = modifier,
    enabled = enabled,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
)

/**
 * Intended to fill the role of primary icon button or fab.
 *
 * @param imageProvider the icon to be drawn in the button
 * @param contentDescription Text used by accessibility services to describe what this image
 * represents. This text should be localized, such as by using
 * androidx.compose.ui.res.stringResource or similar
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param backgroundColor The color to tint the button's background.
 * @param contentColor The color to tint the button's icon.
 */
@Composable
fun SquareIconButton(
    imageProvider: ImageProvider,
    contentDescription: String?,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    backgroundColor: ColorProvider = GlanceTheme.colors.primary,
    contentColor: ColorProvider = GlanceTheme.colors.onPrimary,
) = M3IconButton(
    imageProvider = imageProvider,
    contentDescription = contentDescription,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    shape = IconButtonShape.Square,
    modifier = modifier,
    enabled = enabled,
    onClick = onClick,
)

/**
 * Intended to fill the role of secondary icon button.
 * Background color may be null to have the button display as an icon with a 48x48dp hit area.
 *
 * @param imageProvider the icon to be drawn in the button
 * @param contentDescription Text used by accessibility services to describe what this image
 * represents. This text should be localized, such as by using
 * androidx.compose.ui.res.stringResource or similar
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param backgroundColor The color to tint the button's background. May be null to make background
 * transparent.
 * @param contentColor The color to tint the button's icon.
 * @param key A stable and unique key that identifies the action for this button. This ensures
 * that the correct action is triggered, especially in cases of items that change order. If not
 * provided we use the key that is automatically generated by the Compose runtime, which is unique
 * for every exact code location in the composition tree.
 */
@Composable
fun CircleIconButton(
    imageProvider: ImageProvider,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    backgroundColor: ColorProvider? = GlanceTheme.colors.background,
    contentColor: ColorProvider = GlanceTheme.colors.onSurface,
    key: String? = null
) = CircleIconButton(
    imageProvider = imageProvider,
    contentDescription = contentDescription,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    modifier = modifier,
    enabled = enabled,
    onClick = action(block = onClick, key = key)
)

/**
 * Intended to fill the role of secondary icon button.
 * Background color may be null to have the button display as an icon with a 48x48dp hit area.
 *
 * @param imageProvider the icon to be drawn in the button
 * @param contentDescription Text used by accessibility services to describe what this image
 * represents. This text should be localized, such as by using
 * androidx.compose.ui.res.stringResource or similar
 * @param onClick The action to be performed when this button is clicked.
 * @param modifier The modifier to be applied to this button.
 * @param enabled If false, the button will not be clickable.
 * @param backgroundColor The color to tint the button's background. May be null to make background
 * transparent.
 * @param contentColor The color to tint the button's icon.
 */
@Composable
fun CircleIconButton(
    imageProvider: ImageProvider,
    contentDescription: String?,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
    enabled: Boolean = true,
    backgroundColor: ColorProvider? = GlanceTheme.colors.background,
    contentColor: ColorProvider = GlanceTheme.colors.onSurface,
) = M3IconButton(
    imageProvider = imageProvider,
    contentDescription = contentDescription,
    backgroundColor = backgroundColor,
    contentColor = contentColor,
    shape = IconButtonShape.Circle,
    modifier = modifier,
    enabled = enabled,
    onClick = onClick,
)

private enum class IconButtonShape(
    @DrawableRes val shape: Int,
    @DimenRes val cornerRadius: Int,
    @DrawableRes val ripple: Int,
    val defaultSize: Dp
) {
    Square(
        R.drawable.glance_component_btn_square,
        R.dimen.glance_component_square_icon_button_corners,
        ripple = if (isAtLeastApi31) NoRippleOverride
            else R.drawable.glance_component_square_button_ripple,
        defaultSize = 60.dp
    ),
    Circle(
        R.drawable.glance_component_btn_circle,
        R.dimen.glance_component_circle_icon_button_corners,
        ripple = if (isAtLeastApi31) NoRippleOverride
            else R.drawable.glance_component_circle_button_ripple,
        defaultSize = 48.dp
    )
}

@Composable
private fun M3IconButton(
    imageProvider: ImageProvider,
    contentDescription: String?,
    contentColor: ColorProvider,
    backgroundColor: ColorProvider?,
    shape: IconButtonShape,
    onClick: Action,
    modifier: GlanceModifier,
    enabled: Boolean,
) {

    val backgroundModifier = if (backgroundColor == null)
        GlanceModifier
    else GlanceModifier.background(
        ImageProvider(shape.shape),
        colorFilter = ColorFilter.tint(backgroundColor)
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = GlanceModifier
            .size(shape.defaultSize) // acts as a default if not overridden by [modifier]
            .then(modifier)
            .then(backgroundModifier)
            .clickable(onClick = onClick, rippleOverride = shape.ripple)
            .enabled(enabled)
            .then(maybeRoundCorners(shape.cornerRadius))
    ) {
        Image(
            provider = imageProvider,
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(contentColor),
            modifier = GlanceModifier.size(24.dp)
        )
    }
}

@Composable
private fun M3TextButton(
    text: String,
    onClick: Action,
    modifier: GlanceModifier,
    enabled: Boolean = true,
    icon: ImageProvider?,
    contentColor: ColorProvider,
    @DrawableRes backgroundResource: Int,
    backgroundTint: ColorProvider,
    maxLines: Int,
) {
    val iconSize = 18.dp
    val totalHorizontalPadding = if (icon != null) 24.dp else 16.dp

    val Text = @Composable {
        Text(
            text = text,
            style = TextStyle(color = contentColor, fontSize = 14.sp, FontWeight.Medium),
            maxLines = maxLines
        )
    }

    Box(
        modifier = modifier
            .padding(start = 16.dp, end = totalHorizontalPadding, top = 10.dp, bottom = 10.dp)
            .background(
                imageProvider = ImageProvider(backgroundResource),
                colorFilter = ColorFilter.tint(backgroundTint))
            .enabled(enabled)
            .clickable(
                onClick = onClick,
                rippleOverride = if (isAtLeastApi31) NoRippleOverride
                else R.drawable.glance_component_m3_button_ripple
            )
            .then(maybeRoundCorners(R.dimen.glance_component_button_corners)),
        contentAlignment = Alignment.Center
    ) {

        if (icon != null) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Image(
                    provider = icon,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = GlanceModifier.size(iconSize)
                ) // TODO: do we need a content description for a button icon?
                Spacer(GlanceModifier.width(8.dp))
                Text()
            }
        } else {
            Box(GlanceModifier.size(iconSize)) {
                // for accessibility only: force button to be the same min height as the icon
                // version.
                // remove once b/290677181 is addressed
            }
            Text()
        }
    }
}

private val isAtLeastApi31 get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
private fun maybeRoundCorners(@DimenRes radius: Int) =
    if (isAtLeastApi31)
        GlanceModifier.cornerRadius(radius)
    else GlanceModifier
