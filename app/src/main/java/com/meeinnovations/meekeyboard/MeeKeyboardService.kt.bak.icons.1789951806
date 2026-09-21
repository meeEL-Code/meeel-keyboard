package com.meeinnovations.meekeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MeeKeyboardService : InputMethodService() {

    private val BG_DARK    = Color.parseColor("#0a0f17")
    private val BG_KEY     = Color.parseColor("#1a2336")
    private val BG_SPECIAL = Color.parseColor("#131d2e")
    private val BG_SYMBOL  = Color.parseColor("#0f1724")
    private val BG_PANEL   = Color.parseColor("#080d16")
    private val BG_ITEM    = Color.parseColor("#1a2336")
    private val BG_TOOLBAR = Color.parseColor("#0f1524")
    private val FG_TEXT    = Color.WHITE
    private val FG_BRAND   = Color.parseColor("#60a5fa")
    private val FG_MUTED   = Color.parseColor("#64748b")

    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRepeat: Runnable? = null

    // Which tool panel is open: null = keys, "clipboard", "emoji", ...
    private var activePanel: String? = null

    // Clipboard history (session-only, in-memory)
    private val clipboardHistory = mutableListOf<String>()

    private lateinit var contentContainer: LinearLayout

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(3))
        }

        // Toolbar strip
        root.addView(buildToolbar())

        // Content container (either keys or a panel)
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(contentContainer)

        // Show keys by default
        showKeys()

        return root
    }

    // ─── Toolbar ────────────────────────────────────────────

    private fun buildToolbar(): View {
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG_TOOLBAR)
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }

        bar.addView(toolbarButton("😊", "emoji"))
        bar.addView(toolbarButton("📋", "clipboard"))
        bar.addView(toolbarButton("⌨", "keys"))
        bar.addView(toolbarButton("🎨", "theme"))
        bar.addView(toolbarButton("⚙", "settings"))

        // Spacer
        bar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        // Collapse keyboard
        bar.addView(toolbarButton("⌄", "collapse"))

        return bar
    }

    private fun toolbarButton(symbol: String, action: String): View {
        val b = TextView(this).apply {
            text = symbol
            textSize = 18f
            setTextColor(FG_TEXT)
            gravity = Gravity.CENTER
            val size = dpToPx(40)
            val lp = LinearLayout.LayoutParams(size, size)
            val m = dpToPx(2)
            lp.setMargins(m, m, m, m)
            layoutParams = lp
            setPadding(0, 0, 0, 0)

            setOnClickListener { v ->
                haptic(v)
                when (action) {
                    "clipboard" -> showClipboardPanel()
                    "emoji"     -> showEmojiPanel()
                    "keys"      -> showKeys()
                    "theme"     -> showThemePanel()
                    "settings"  -> showSettingsPanel()
                    "collapse"  -> requestHideSelf(0)
                }
            }
        }
        return b
    }

    // ─── Key panels ────────────────────────────────────────

    private fun showKeys() {
        activePanel = null
        contentContainer.removeAllViews()

        val r1 = row()
        listOf("1","2","3","4","5","6","7","8","9","0").forEach { r1.addView(letterKey(it)) }
        r1.addView(backspaceKey())
        contentContainer.addView(r1)

        val r2 = row()
        listOf("q","w","e","r","t","y","u","i","o","p").forEach { r2.addView(letterKey(it)) }
        contentContainer.addView(r2)

        val r3 = row()
        listOf("a","s","d","f","g","h","j","k","l").forEach { r3.addView(letterKey(it)) }
        r3.addView(specialKey("⏎", "Enter", 1.4f))
        contentContainer.addView(r3)

        val r4 = row()
        listOf("z","x","c","v","b","n","m",".",",").forEach { r4.addView(letterKey(it)) }
        contentContainer.addView(r4)

        val r5 = row()
        r5.addView(symbolKey("#"))
        r5.addView(symbolKey("[]"))
        r5.addView(symbolKey("-"))
        r5.addView(symbolKey("!"))
        r5.addView(spaceKey())
        contentContainer.addView(r5)
    }

    // ─── Clipboard panel ────────────────────────────────────

    private fun showClipboardPanel() {
        activePanel = "clipboard"
        contentContainer.removeAllViews()

        // Header row: title + Clear
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
        }
        header.addView(TextView(this).apply {
            text = "Clipboard"
            setTextColor(FG_BRAND)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = "Clear"
            setTextColor(FG_MUTED)
            textSize = 12f
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            setOnClickListener { v ->
                haptic(v)
                clipboardHistory.clear()
                showClipboardPanel()
            }
        })
        contentContainer.addView(header)

        // Refresh from system clipboard (last copied item)
        syncSystemClipboard()

        // Empty state
        if (clipboardHistory.isEmpty()) {
            contentContainer.addView(TextView(this).apply {
                text = "No clipboard items yet.\nLong-press text to copy, or use Paste below."
                setTextColor(FG_MUTED)
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(dpToPx(20), dpToPx(30), dpToPx(20), dpToPx(20))
            })
        } else {
            val scroll = ScrollView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(180)
                )
            }
            val list = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dpToPx(6), 0, dpToPx(6), dpToPx(6))
            }

            clipboardHistory.forEach { item ->
                list.addView(clipboardItem(item))
            }

            scroll.addView(list)
            contentContainer.addView(scroll)
        }

        // Bottom row: Paste from system
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
        }
        bottom.addView(actionBtn("Paste from clipboard") {
            val clip = clipboardManager().primaryClip
            if (clip != null && clip.itemCount > 0) {
                val txt = clip.getItemAt(0).coerceToText(this).toString()
                if (txt.isNotEmpty()) {
                    clipboardHistory.remove(txt)
                    clipboardHistory.add(0, txt)
                    currentInputConnection?.commitText(txt, 1)
                }
            }
        })
        bottom.addView(actionBtn("Keys") { showKeys() })
        contentContainer.addView(bottom)
    }

    private fun clipboardItem(text: String): View {
        val preview = if (text.length > 100) text.substring(0, 100) + "…" else text
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(BG_ITEM)
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.setMargins(dpToPx(2), dpToPx(3), dpToPx(2), dpToPx(3))
            layoutParams = lp

            addView(TextView(this@MeeKeyboardService).apply {
                text = preview
                setTextColor(FG_TEXT)
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { v ->
                    haptic(v)
                    currentInputConnection?.commitText(text, 1)
                }
            })

            addView(TextView(this@MeeKeyboardService).apply {
                text = "✕"
                setTextColor(FG_MUTED)
                textSize = 14f
                setPadding(dpToPx(10), dpToPx(4), dpToPx(4), dpToPx(4))
                setOnClickListener { v ->
                    haptic(v)
                    clipboardHistory.remove(text)
                    showClipboardPanel()
                }
            })
        }
    }

    private fun syncSystemClipboard() {
        val clip = clipboardManager().primaryClip ?: return
        if (clip.itemCount == 0) return
        val txt = clip.getItemAt(0).coerceToText(this).toString()
        if (txt.isEmpty()) return
        clipboardHistory.remove(txt)
        clipboardHistory.add(0, txt)
        // Cap at 20
        while (clipboardHistory.size > 20) clipboardHistory.removeAt(clipboardHistory.size - 1)
    }

    private fun clipboardManager(): ClipboardManager =
        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // ─── Placeholder panels ─────────────────────────────────

    private fun showEmojiPanel() {
        activePanel = "emoji"
        contentContainer.removeAllViews()
        contentContainer.addView(TextView(this).apply {
            text = "Emoji panel coming soon 🌱"
            setTextColor(FG_MUTED)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(40), 0, dpToPx(40))
        })
        val back = row()
        back.addView(actionBtn("Back to keys") { showKeys() })
        contentContainer.addView(back)
    }

    private fun showThemePanel() {
        activePanel = "theme"
        contentContainer.removeAllViews()
        contentContainer.addView(TextView(this).apply {
            text = "Theme settings coming soon 🎨"
            setTextColor(FG_MUTED)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(40), 0, dpToPx(40))
        })
        val back = row()
        back.addView(actionBtn("Back to keys") { showKeys() })
        contentContainer.addView(back)
    }

    private fun showSettingsPanel() {
        activePanel = "settings"
        contentContainer.removeAllViews()
        contentContainer.addView(TextView(this).apply {
            text = "Settings coming soon ⚙"
            setTextColor(FG_MUTED)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(40), 0, dpToPx(40))
        })
        val back = row()
        back.addView(actionBtn("Back to keys") { showKeys() })
        contentContainer.addView(back)
    }

    // ─── Helpers ────────────────────────────────────────────

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
            if (action == "Enter") currentInputConnection?.commitText("\n", 1)
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
    }

    private fun actionBtn(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            setTextColor(FG_TEXT)
            setBackgroundColor(BG_ITEM)
            isAllCaps = false
            val lp = LinearLayout.LayoutParams(0, dpToPx(42), 1f)
            val m = dpToPx(3)
            lp.setMargins(m, m, m, m)
            layoutParams = lp
            setOnClickListener { v -> haptic(v); onClick() }
        }
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        super.onDestroy()
    }
}
