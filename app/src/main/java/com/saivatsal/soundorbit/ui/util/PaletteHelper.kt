package com.saivatsal.soundorbit.ui.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.saivatsal.soundorbit.ui.theme.EmeraldGreenBright
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PaletteHelper {
    suspend fun extractAccentColor(
        context: Context,
        imageUrl: String?,
        fallback: Color = EmeraldGreenBright
    ): Color = withContext(Dispatchers.IO) {
        if (imageUrl.isNullOrBlank()) return@withContext fallback

        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .build()

            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap: Bitmap? = try {
                    result.image.toBitmap()
                } catch (e: Exception) {
                    null
                }

                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.lightVibrantSwatch
                        ?: palette.darkVibrantSwatch
                        ?: palette.mutedSwatch

                    if (swatch != null) {
                        return@withContext Color(swatch.rgb)
                    }
                }
            }
        } catch (e: Exception) {
            // Keep fallback on error
        }
        return@withContext fallback
    }
}
