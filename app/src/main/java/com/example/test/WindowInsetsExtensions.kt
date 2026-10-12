package com.example.test

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/** Apply system/cutout insets once to the screen root; required by the target SDK's edge-to-edge behavior. */
fun AppCompatActivity.applySystemBarInsets() {
    val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
    val left = root.paddingLeft
    val top = root.paddingTop
    val right = root.paddingRight
    val bottom = root.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.updatePadding(left = left + bars.left, top = top + bars.top, right = right + bars.right, bottom = bottom + bars.bottom)
        // Children such as BottomNavigationView must not apply the same system-bar padding again.
        WindowInsetsCompat.Builder(insets)
            .setInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(), Insets.NONE)
            .build()
    }
    ViewCompat.requestApplyInsets(root)
}
