package com.meeinnovations.meekeyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
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
    private val BG_KEY     = Color.parseColor("#2A3441")
    private val BG_SPECIAL = Color.parseColor("#1F2937")
    private val BG_SYMBOL  = Color.parseColor("#1A2336")
    private val BG_PANEL   = Color.parseColor("#080d16")
    private val BG_ITEM    = Color.parseColor("#2A3441")
    private val BG_TOOLBAR = Color.parseColor("#0f1524")
    private val FG_TEXT    = Color.parseColor("#E8EAED")
    private val FG_BRAND   = Color.parseColor("#60a5fa")
    private val FG_MUTED   = Color.parseColor("#64748b")

    private val handler = Handler(Looper.getMainLooper())
    private var backspaceRepeat: Runnable? = null

    // Which tool panel is open: null = keys, "clipboard", "emoji", ...
    private var activePanel: String? = null

    // Clipboard history
    private val clipboardRecent = mutableListOf<String>()
    private val clipboardPinned = mutableListOf<String>()

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("meekeyboard", Context.MODE_PRIVATE)
    }

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        onSystemClipboardChanged()
    }
    private var clipListenerRegistered = false

    private lateinit var contentContainer: LinearLayout

    override fun onStartInput(attribute: android.view.inputmethod.EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        loadClipboardFromPrefs()
        registerClipListener()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        unregisterClipListener()
    }

    private fun registerClipListener() {
        if (clipListenerRegistered) return
        try {
            clipboardManager().addPrimaryClipChangedListener(clipListener)
            clipListenerRegistered = true
        } catch (_: Throwable) {}
    }

    private fun unregisterClipListener() {
        if (!clipListenerRegistered) return
        try {
            clipboardManager().removePrimaryClipChangedListener(clipListener)
        } catch (_: Throwable) {}
        clipListenerRegistered = false
    }

    private fun onSystemClipboardChanged() {
        val clip = clipboardManager().primaryClip ?: return
        if (clip.itemCount == 0) return
        val txt = clip.getItemAt(0).coerceToText(this).toString().trim()
        if (txt.isEmpty()) return
        if (clipboardPinned.contains(txt)) return
        clipboardRecent.remove(txt)
        clipboardRecent.add(0, txt)
        while (clipboardRecent.size > 30) clipboardRecent.removeAt(clipboardRecent.size - 1)
        saveClipboardToPrefs()
        // If clipboard panel is open, refresh it
        if (activePanel == "clipboard") showClipboardPanel()
    }

    private fun saveClipboardToPrefs() {
        try {
            prefs.edit()
                .putString("recent", clipboardRecent.joinToString("\u0001"))
                .putString("pinned", clipboardPinned.joinToString("\u0001"))
                .apply()
        } catch (_: Throwable) {}
    }

    private fun loadClipboardFromPrefs() {
        try {
            if (clipboardRecent.isEmpty()) {
                val r = prefs.getString("recent", "") ?: ""
                if (r.isNotEmpty()) {
                    clipboardRecent.clear()
                    clipboardRecent.addAll(r.split("\u0001").filter { it.isNotEmpty() })
                }
            }
            if (clipboardPinned.isEmpty()) {
                val p = prefs.getString("pinned", "") ?: ""
                if (p.isNotEmpty()) {
                    clipboardPinned.clear()
                    clipboardPinned.addAll(p.split("\u0001").filter { it.isNotEmpty() })
                }
            }
        } catch (_: Throwable) {}
    }

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

        bar.addView(toolbarButton(R.drawable.ic_emoji, "emoji"))
        bar.addView(toolbarButton(R.drawable.ic_clipboard, "clipboard"))
        bar.addView(toolbarButton(R.drawable.ic_keyboard, "keys"))
        bar.addView(toolbarButton(R.drawable.ic_theme, "theme"))
        bar.addView(toolbarButton(R.drawable.ic_settings, "settings"))

        // Spacer
        bar.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        })

        // Collapse keyboard
        bar.addView(toolbarButton(R.drawable.ic_collapse, "collapse"))

        return bar
    }

    private fun toolbarButton(iconRes: Int, action: String): View {
        val img = android.widget.ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(FG_TEXT)
            val size = dpToPx(24)
            val pad = dpToPx(8)
            val lp = LinearLayout.LayoutParams(size + pad * 2, size + pad * 2)
            val m = dpToPx(2)
            lp.setMargins(m, m, m, m)
            layoutParams = lp
            setPadding(pad, pad, pad, pad)

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
        return img
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

        // Header row — keyboard (back) + title + pin toggle + trash
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(6))
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(iconButton(R.drawable.ic_keyboard) { showKeys() })
        header.addView(TextView(this).apply {
            text = "Clipboard"
            setTextColor(FG_TEXT)
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(iconButton(R.drawable.ic_pin) {
            android.widget.Toast.makeText(this, "Pin items via long-press", android.widget.Toast.LENGTH_SHORT).show()
        })
        header.addView(iconButton(R.drawable.ic_trash) {
            clipboardRecent.clear()
            clipboardPinned.clear()
            saveClipboardToPrefs()
            showClipboardPanel()
        })
        contentContainer.addView(header)

        // Sync system clipboard
        syncSystemClipboard()

        // Empty state
        if (clipboardRecent.isEmpty() && clipboardPinned.isEmpty()) {
            contentContainer.addView(TextView(this).apply {
                text = "No clipboard items yet.\nCopy some text and it will appear here."
                setTextColor(FG_MUTED)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(dpToPx(24), dpToPx(50), dpToPx(24), dpToPx(30))
            })
            return
        }

        val scrollWrap = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dpToPx(6))
        }

        // Recent section
        if (clipboardRecent.isNotEmpty()) {
            body.addView(sectionHeader("Recent"))
            val recentRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(6), 0, dpToPx(6), dpToPx(8))
            }
            clipboardRecent.forEach { text ->
                recentRow.addView(clipboardCard(text))
            }
            val hScroll = android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(recentRow)
            }
            body.addView(hScroll)
        }

        // Pinned section
        if (clipboardPinned.isNotEmpty()) {
            body.addView(sectionHeader("Pinned"))
            val pinnedRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dpToPx(6), 0, dpToPx(6), dpToPx(8))
            }
            clipboardPinned.forEach { text ->
                pinnedRow.addView(clipboardCard(text, pinned = true))
            }
            val hScroll = android.widget.HorizontalScrollView(this).apply {
                isHorizontalScrollBarEnabled = false
                addView(pinnedRow)
            }
            body.addView(hScroll)
        }

        // Bottom row
        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(4))
        }
        bottom.addView(actionBtn("Paste from clipboard") {
            val clip = clipboardManager().primaryClip
            if (clip != null && clip.itemCount > 0) {
                val txt = clip.getItemAt(0).coerceToText(this).toString()
                if (txt.isNotEmpty()) {
                    clipboardRecent.remove(txt)
                    clipboardRecent.add(0, txt)
                    while (clipboardRecent.size > 20) clipboardRecent.removeAt(clipboardRecent.size - 1)
                    currentInputConnection?.commitText(txt, 1)
                    showClipboardPanel()
                }
            }
        })
        body.addView(bottom)

        scrollWrap.addView(body)
        scrollWrap.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        contentContainer.addView(scrollWrap)
    }

    private fun sectionHeader(title: String): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(4))
        }
        row.addView(TextView(this).apply {
            text = title
            setTextColor(FG_BRAND)
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(this).apply {
            text = "Show more"
            setTextColor(FG_MUTED)
            textSize = 11f
        })
        return row
    }

    private fun iconButton(iconRes: Int, onClick: () -> Unit): View {
        return android.widget.ImageView(this).apply {
            setImageResource(iconRes)
            setColorFilter(FG_TEXT)
            val size = dpToPx(36)
            layoutParams = LinearLayout.LayoutParams(size, size)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setOnClickListener { v -> haptic(v); onClick() }
        }
    }

    private fun clipboardCard(text: String, pinned: Boolean = false): View {
        val preview = if (text.length > 60) text.substring(0, 60).trim() + "…" else text

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_ITEM)
            val w = dpToPx(150)
            val h = dpToPx(70)
            val lp = LinearLayout.LayoutParams(w, h)
            val m = dpToPx(4)
            lp.setMargins(m, m, m, m)
            layoutParams = lp
            setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8))

            addView(TextView(this@MeeKeyboardService).apply {
                this.text = preview
                setTextColor(FG_TEXT)
                textSize = 12f
                maxLines = 3
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
                )
            })
        }

        // Tap → paste
        card.setOnClickListener { v ->
            haptic(v)
            currentInputConnection?.commitText(text, 1)
        }

        // Long-press → pin/unpin or delete menu
        card.setOnLongClickListener { v ->
            haptic(v)
            showCardMenu(text, pinned, v)
            true
        }

        return card
    }

    private fun showCardMenu(text: String, pinned: Boolean, anchor: View) {
        val menu = android.widget.PopupMenu(this, anchor)
        if (pinned) {
            menu.menu.add(0, 1, 0, "Unpin")
        } else {
            menu.menu.add(0, 1, 0, "Pin")
        }
        menu.menu.add(0, 2, 1, "Delete")
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    if (pinned) {
                        clipboardPinned.remove(text)
                        clipboardRecent.add(0, text)
                    } else {
                        clipboardRecent.remove(text)
                        clipboardPinned.add(0, text)
                    }
                    saveClipboardToPrefs()
                    showClipboardPanel()
                    true
                }
                2 -> {
                    clipboardRecent.remove(text)
                    clipboardPinned.remove(text)
                    saveClipboardToPrefs()
                    showClipboardPanel()
                    true
                }
                else -> false
            }
        }
        menu.show()
    }

    private fun syncSystemClipboard() {
        val clip = clipboardManager().primaryClip ?: return
        if (clip.itemCount == 0) return
        val txt = clip.getItemAt(0).coerceToText(this).toString()
        if (txt.isEmpty()) return
        // Skip if already pinned
        if (clipboardPinned.contains(txt)) return
        clipboardRecent.remove(txt)
        clipboardRecent.add(0, txt)
        while (clipboardRecent.size > 20) clipboardRecent.removeAt(clipboardRecent.size - 1)
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
        LinearLayout.LayoutParams(0, dpToPx(44), weight).apply {
            val m = dpToPx(3)
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
        textSize = 15f
        setTextColor(FG_TEXT)
        setBackgroundResource(R.drawable.bg_key)
        setPadding(0, 0, 0, 0)
        isAllCaps = false
        stateListAnimator = null
        layoutParams = baseParams(1f)
        setOnClickListener { v -> haptic(v); commit(label) }
    }

    private fun symbolKey(label: String): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(FG_BRAND)
        setBackgroundResource(R.drawable.bg_key_symbol)
        setPadding(0, 0, 0, 0)
        stateListAnimator = null
        layoutParams = baseParams(1f)
        setOnClickListener { v -> haptic(v); commit(label) }
    }

    private fun specialKey(label: String, action: String, weight: Float = 1f): Button = Button(this).apply {
        text = label
        textSize = 15f
        setTextColor(FG_TEXT)
        setBackgroundResource(R.drawable.bg_key_special)
        setPadding(0, 0, 0, 0)
        stateListAnimator = null
        layoutParams = baseParams(weight)
        setOnClickListener { v ->
            haptic(v)
            if (action == "Enter") currentInputConnection?.commitText("\n", 1)
        }
    }

    private fun backspaceKey(): Button = Button(this).apply {
        text = "⌫"
        textSize = 15f
        setTextColor(FG_TEXT)
        setBackgroundResource(R.drawable.bg_key_special)
        setPadding(0, 0, 0, 0)
        stateListAnimator = null
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
        textSize = 13f
        setTextColor(FG_TEXT)
        setBackgroundResource(R.drawable.bg_key_space)
        setPadding(0, 0, 0, 0)
        stateListAnimator = null
        layoutParams = baseParams(4f)
        setOnClickListener { v -> haptic(v); commit(" ") }
    }

    private fun actionBtn(label: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            setTextColor(FG_TEXT)
            setBackgroundResource(R.drawable.bg_key_special)
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            stateListAnimator = null
            val lp = LinearLayout.LayoutParams(0, dpToPx(44), 1f)
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
        unregisterClipListener()
        super.onDestroy()
    }
}
