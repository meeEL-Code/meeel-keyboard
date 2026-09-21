package com.meeinnovations.meekeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView

class MeeKeyboardService : InputMethodService() {

    private val BG_DARK    = Color.parseColor("#0a0f17")
    private val BG_KEY     = Color.parseColor("#1a2336")
    private val BG_SPECIAL = Color.parseColor("#131d2e")
    private val BG_SYMBOL  = Color.parseColor("#0f1724")
    private val BG_POPUP   = Color.parseColor("#131d2e")
    private val BG_ITEM    = Color.parseColor("#1a2336")
    private val FG_TEXT    = Color.WHITE
    private val FG_BRAND   = Color.parseColor("#60a5fa")
    private val FG_MUTED   = Color.parseColor("#94a3b8")

    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRepeat: Runnable? = null
    private var popup: PopupWindow? = null

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
            setPadding(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3))
        }

        val r1 = row()
        listOf("1","2","3","4","5","6","7","8","9","0").forEach {
            r1.addView(letterKey(it))
        }
        r1.addView(backspaceKey())
        root.addView(r1)

        val r2 = row()
        listOf("q","w","e","r","t","y","u","i","o","p").forEach {
            r2.addView(letterKey(it))
        }
        root.addView(r2)

        val r3 = row()
        listOf("a","s","d","f","g","h","j","k","l").forEach {
            r3.addView(letterKey(it))
        }
        r3.addView(specialKey("⏎", "Enter", 1.4f))
        root.addView(r3)

        val r4 = row()
        listOf("z","x","c","v","b","n","m",".",",").forEach {
            r4.addView(letterKey(it))
        }
        root.addView(r4)

        val r5 = row()
        r5.addView(symbolKey("#"))
        r5.addView(symbolKey("[]"))
        r5.addView(symbolKey("-"))
        r5.addView(symbolKey("!"))
        r5.addView(spaceKey())
        root.addView(r5)

        return root
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun row(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
    }

    private fun baseParams(weight: Float): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dpToPx(42), weight).apply {
            val m = dpToPx(2)
            setMargins(m, m, m, m)
        }

    private fun haptic(v: View) {
        v.performHapticFeedback(
            HapticFeedbackConstants.KEYBOARD_TAP,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    private fun letterKey(label: String): Button = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_KEY)
        isAllCaps = false
        layoutParams = baseParams(1f)
        setOnClickListener { v -> haptic(v); commit(label) }
    }

    private fun symbolKey(label: String): Button = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(FG_BRAND)
        setBackgroundColor(BG_SYMBOL)
        layoutParams = baseParams(1f)
        setOnClickListener { v -> haptic(v); commit(label) }
    }

    private fun specialKey(label: String, action: String, weight: Float = 1f): Button = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_SPECIAL)
        layoutParams = baseParams(weight)
        setOnClickListener { v ->
            haptic(v)
            val ic = currentInputConnection ?: return@setOnClickListener
            if (action == "Enter") ic.commitText("\n", 1)
        }
    }

    private fun backspaceKey(): Button = Button(this).apply {
        text = "⌫"
        textSize = 14f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_SPECIAL)
        layoutParams = baseParams(1.4f)

        setOnClickListener { v ->
            haptic(v)
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        setOnLongClickListener { v ->
            haptic(v)
            startBackspaceRepeat()
            true
        }
        setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_UP ||
                event.action == android.view.MotionEvent.ACTION_CANCEL) {
                stopBackspaceRepeat()
            }
            false
        }
    }

    private fun startBackspaceRepeat() {
        stopBackspaceRepeat()
        val r = object : Runnable {
            override fun run() {
                currentInputConnection?.deleteSurroundingText(1, 0)
                handler.postDelayed(this, 60L)
            }
        }
        backspaceRepeat = r
        handler.postDelayed(r, 250L)
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeat?.let { handler.removeCallbacks(it) }
        backspaceRepeat = null
    }

    private fun spaceKey(): Button = Button(this).apply {
        text = "Space"
        textSize = 12f
        setTextColor(FG_TEXT)
        setBackgroundColor(BG_SYMBOL)
        layoutParams = baseParams(4f)

        setOnClickListener { v -> haptic(v); commit(" ") }
        // Long-press space → clipboard menu
        setOnLongClickListener { v ->
            haptic(v)
            showClipboardPopup(v)
            true
        }
    }

    // ─── Clipboard popup ────────────────────────────────────

    private fun clipboard(): ClipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    private fun showClipboardPopup(anchor: View) {
        dismissPopup()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_POPUP)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }

        // Header
        val header = TextView(this).apply {
            text = "Clipboard"
            setTextColor(FG_BRAND)
            textSize = 11f
            setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(6))
        }
        container.addView(header)

        // Action row: Copy / Paste / Select all
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actionRow.addView(actionButton("Copy") {
            haptic(anchor)
            val ic = currentInputConnection ?: return@actionButton
            val sel = ic.getSelectedText(0)
            if (!sel.isNullOrEmpty()) {
                clipboard().setPrimaryClip(ClipData.newPlainText("meeKeyboard", sel))
                commit("")
                showToast("Copied")
            } else {
                showToast("Select text first")
            }
            dismissPopup()
        })
        actionRow.addView(actionButton("Paste") {
            haptic(anchor)
            val clip = clipboard().primaryClip
            if (clip != null && clip.itemCount > 0) {
                val txt = clip.getItemAt(0).coerceToText(this).toString()
                currentInputConnection?.commitText(txt, 1)
            }
            dismissPopup()
        })
        actionRow.addView(actionButton("Select all") {
            haptic(anchor)
            currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
            dismissPopup()
        })
        container.addView(actionRow)

        // Clipboard history — up to 5 items
        val clip = clipboard().primaryClip
        if (clip != null && clip.itemCount > 0) {
            val txt = clip.getItemAt(0).coerceToText(this).toString()
            if (txt.isNotEmpty()) {
                val divider = TextView(this).apply {
                    text = ""
                    setPadding(0, dpToPx(6), 0, dpToPx(6))
                    setBackgroundColor(Color.parseColor("#1f2937"))
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1)
                    )
                }
                container.addView(divider)

                val label = TextView(this).apply {
                    text = "Tap to paste"
                    setTextColor(FG_MUTED)
                    textSize = 10f
                    setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(2))
                }
                container.addView(label)

                val preview = TextView(this).apply {
                    text = if (txt.length > 80) txt.substring(0, 80) + "…" else txt
                    setTextColor(FG_TEXT)
                    textSize = 13f
                    setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
                    setBackgroundColor(BG_ITEM)
                    setOnClickListener {
                        haptic(this)
                        currentInputConnection?.commitText(txt, 1)
                        dismissPopup()
                    }
                }
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
                preview.layoutParams = lp
                container.addView(preview)
            }
        }

        // Popup wrap
        val scroll = ScrollView(this).apply {
            addView(container)
            isFillViewport = true
        }

        val width = dpToPx(280)
        val pw = PopupWindow(scroll, width, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pw.isOutsideTouchable = true
        pw.elevation = dpToPx(8).toFloat()

        // Show above the space bar
        pw.showAsDropDown(anchor, 0, -anchor.height - dpToPx(8), Gravity.START)
        popup = pw
    }

    private fun actionButton(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(FG_TEXT)
            setBackgroundColor(BG_ITEM)
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(0, dpToPx(40), 1f)
            val m = dpToPx(3)
            lp.setMargins(m, m, m, m)
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun dismissPopup() {
        popup?.dismiss()
        popup = null
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun commit(text: String) {
        val ic = currentInputConnection ?: return
        ic.commitText(text, 1)
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        dismissPopup()
        super.onDestroy()
    }
}
