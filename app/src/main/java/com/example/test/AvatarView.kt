package com.example.test

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.widget.ImageView
import kotlin.math.max
import kotlin.math.min

/** Circular display only: original Commons files and their credits remain untouched. */
object AvatarView {
    private val portraits = LruCache<Int, Bitmap>(8)

    fun bind(view: ImageView, resourceId: Int) {
        if (resourceId == R.drawable.avatar_default) {
            view.setImageResource(resourceId)
            CompactText.circle(view)
            return
        }
        val size = (64 * view.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val bitmap = portraits.get(resourceId) ?: decode(view.resources, resourceId, size).also {
            portraits.put(resourceId, it)
        }
        view.scaleType = ImageView.ScaleType.FIT_CENTER
        view.setImageDrawable(CircularPortrait(bitmap, size))
    }

    private fun decode(resources: Resources, resourceId: Int, size: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(resources, resourceId, bounds)
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
        return requireNotNull(BitmapFactory.decodeResource(resources, resourceId,
            BitmapFactory.Options().apply { inSampleSize = sample; inScaled = false }))
    }

    private class CircularPortrait(private val bitmap: Bitmap, private val size: Int) : Drawable() {
        private val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = this@CircularPortrait.shader }
        private val matrix = Matrix()
        override fun onBoundsChange(bounds: Rect) {
            val scale = max(bounds.width().toFloat() / bitmap.width, bounds.height().toFloat() / bitmap.height)
            matrix.setScale(scale, scale)
            matrix.postTranslate(bounds.centerX() - bitmap.width * scale / 2,
                bounds.centerY() - bitmap.height * scale / 2)
            shader.setLocalMatrix(matrix)
        }
        override fun draw(canvas: Canvas) {
            canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), min(bounds.width(), bounds.height()) / 2f, paint)
        }
        override fun getIntrinsicWidth() = size
        override fun getIntrinsicHeight() = size
        override fun setAlpha(alpha: Int) { paint.alpha = alpha; invalidateSelf() }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter; invalidateSelf() }
        @Deprecated("Required by Drawable") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
