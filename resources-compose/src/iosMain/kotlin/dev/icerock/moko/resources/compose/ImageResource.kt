/*
 * Copyright 2023 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.resources.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import dev.icerock.moko.resources.ImageResource
import dev.icerock.moko.resources.compose.internal.toSkiaImage
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGImageRef
import platform.Foundation.NSFileManager
import platform.Foundation.NSBundle
import platform.UIKit.UIImage
import platform.UIKit.UIScreen

@Composable
actual fun painterResource(imageResource: ImageResource): Painter {
    @OptIn(ExperimentalResourceApi::class)
    if (imageResource.originalExtension != null && imageResource.originalExtension != "svg") {
        val currentScreenScale = UIScreen.mainScreen.scale.toInt()
        val filepath = remember(imageResource, currentScreenScale) {
            val originalExtension = imageResource.originalExtension
            val scaleSearchOrder = when (currentScreenScale) {
                2 -> arrayOf(2, 3, 1)
                1 -> arrayOf(1, 3, 2)
                else -> arrayOf(3, 2, 1)
            }
            for (currentScale in scaleSearchOrder) {
                val currentPath =
                    "MR/images/${imageResource.assetImageName}@${currentScale}x.$originalExtension"
                // TODO: Check for whether the bundle is statically linked or not
                val absolutePath = NSBundle.mainBundle.resourcePath + "/" + currentPath
                if (NSFileManager.defaultManager.fileExistsAtPath(absolutePath)) {
                    return@remember currentPath
                }
            }
            throw Exception("Missing image resource: ${imageResource.assetImageName}")
        }
        return org.jetbrains.compose.resources.painterResource(filepath)
    }

    return remember(imageResource) {
        val uiImage: UIImage = imageResource.toUIImage()
            ?: throw IllegalArgumentException("can't read UIImage of $imageResource")

        val cgImage: CGImageRef = uiImage.CGImage()
            ?: throw IllegalArgumentException("can't read CGImage of $imageResource")

        val skiaImage: Image = cgImage.toSkiaImage()

        BitmapPainter(image = skiaImage.toComposeImageBitmap())
    }
}
