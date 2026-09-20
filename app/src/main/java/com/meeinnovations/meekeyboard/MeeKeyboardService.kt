package com.meeinnovations.meekeyboard

import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.graphics.Color

class MeeKeyboardService : InputMethodService() {

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0a0f17"))
            setPadding(8, 8, 8, 16)
        }

        // Row 1: small labels (proves it's working)
        val title = TextView(this).apply {
            text = "meeKeyboard"
            setTextColor(Color.parseColor("#60a5fa"))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 8)
        }
        root.addView(title)

        // Row: a few test keys
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(makeKey("a"))
        row.addView(makeKey("b"))
        row.addView(makeKey("c"))
        row.addView(makeKey("Space", 3f))

        root.addView(row, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        return root
    }

    private fun makeKey(label: String, weight: Float = 1f): Button {
        return Button(this).apply {
            text = label
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1a2336"))
            val params = LinearLayout.LayoutParams(0, 140, weight)
            params.setMargins(4, 4, 4, 4)
            layoutParams = params
            setOnClickListener {
                val ic = currentInputConnection ?: return@setOnClickListener
                val text = if (label == "Space") " " else label
                ic.commitText(text, 1)
            }
        }
    }
}
