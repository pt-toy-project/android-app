package com.example.test

import android.graphics.Outline
import android.text.TextUtils
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView

object CompactText {
    fun apply(view: TextView) {
        val expanded = view.resources.configuration.fontScale >= 1.3f
        view.maxLines = if (expanded) Int.MAX_VALUE else 1
        view.ellipsize = if (expanded) null else TextUtils.TruncateAt.END
    }
    fun circle(view: ImageView) {
        view.scaleType = ImageView.ScaleType.CENTER_CROP
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) { outline.setOval(0, 0, v.width, v.height) }
        }
        view.clipToOutline = true
    }
}
