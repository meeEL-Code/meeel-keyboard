package com.meeinnovations.meekeyboard

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MeeKeyboardService : InputMethodService() {

    private val BG_DARK   = Color.parseColor("#0a0f17")
    private val BG_KEY    = Color.parseColor("#1a2336")
    private val BG_SPECIAL= Color.parseColor("#131d2e")
    private val BG_SYMBOL = Color.parseColor("#0f1724")
    private val FG_TEXT   = Color.WHITE
    private val FG_BRAND  = Color.parseColor("#60a5fa")

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
            setPadding(4, 4, 4, 8)
        }

        // Brand label
        root.addView(TextView(this).apply {
            text = "meeKeyboard"
            setTextColor(FG_BRAND)
            textSize = 10f
            gravity = Gravity.CENTER
            setPadding(0, 2, 0, 4)
        })

        // Row 1 — numbers
        val r1 = row()
        listOf("1","2","3","4","5","6","7","8","9","0").forEach {
            r1.addView(letterKey(it))
        }
        r1.addView(specialKey("⌫", "Backspace", 1.4f))
        root.addView(r1)

        // Row 2 — qwerty
        val r2 = row()
        listOf("q","w","e","r","t","y","u","i","o","p").forEach {
            r2.addView(letterKey(it))
        }
        root.addView(r2)

        // Row 3 — asdf
        val r3 = row()
        listOf("a","s","d","f","g","h","j","k","l").forEach {
            r3.addView(letterKey(it))
        }
        r3.addView(specialKey("⏎", "Enter", 1.4f))
        root.addView(r3)

        // Row 4 — zxcv + punctuation
        val r4 = row()
        listOf("z","x","c","v","b","n","m",".",",").forEach {
            r4.addView(letterKey(it))
        }
        root.addView(r4)

        // Row 5 — meeEL symbols + space
        val r5 = row()
        r5.addView(symbolKey("#"))
        r5.addView(symbolKey("[]"))
        r5.addView(symbolKey("-"))
        r5.addView(symbolKey("!"))
        r5.addView(spaceKey())
        root.addView(r5)

        return root
    }

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun baseParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, 130, weight).apply {
            setMargins(3, 3, 3, 3)
        }

    private fun letterKey(label: String): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_KEY)
        isAllCaps = false
        layoutParams = baseParams(1f)
        setOnClickListener { commit(label) }
    }

    private fun symbolKey(label: String): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(FG_BRAND)
        setBackgroundColor(BG_SYMBOL)
        layoutParams = baseParams(1f)
        setOnClickListener { commit(label) }
    }

    private fun specialKey(label: String, action: String, weight: Float = 1f): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_SPECIAL)
        layoutParams = baseParams(weight)
        setOnClickListener {
            val ic = currentInputConnection ?: return@setOnClickListener
            when (action) {
                "Backspace" -> ic.deleteSurroundingText(1, 0)
                "Enter"     -> ic.commitText("\n", 1)
            }
        }
    }

    private fun spaceKey(): Button = Button(this).apply {
        text = "Space"
        textSize = 13f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_SYMBOL)
        layoutParams = baseParams(4f)
        setOnClickListener { commit(" ") }
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }
}
