package com.meeinnovations.meekeyboard

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.Button
import kotlin.math.hypot

class SwipeLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onSwipe: ((List<Pair<Float, Float>>) -> Unit)? = null
    var swipeThresholdPx: Float = 80f

    private val path = mutableListOf<Pair<Float, Float>>()
    private var isSwipe = false
    private var startX = 0f
    private var startY = 0f

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                path.clear()
                path.add(ev.x to ev.y)
                startX = ev.x
                startY = ev.y
                isSwipe = false
            }
            MotionEvent.ACTION_MOVE -> {
                path.add(ev.x to ev.y)
                if (!isSwipe && path.size > 3) {
                    val d = hypot(
                        (ev.x - startX).toDouble(),
                        (ev.y - startY).toDouble()
                    )
                    if (d > swipeThresholdPx) {
                        isSwipe = true
                        // Tell children to cancel their taps
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> path.add(ev.x to ev.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isSwipe && path.size > 5) {
                    onSwipe?.invoke(path.toList())
                }
                path.clear()
                isSwipe = false
            }
        }
        return true
    }

    // Look up which key is under (x, y) in this container's coordinate space
    fun keyUnder(x: Float, y: Float): Button? {
        for (r in 0 until childCount) {
            val row = getChildAt(r)
            if (row !is LinearLayout) continue
            val rowLeft = row.x
            val rowTop = row.y
            for (k in 0 until row.childCount) {
                val child = row.getChildAt(k)
                if (child !is Button) continue
                val bx = rowLeft + child.x
                val by = rowTop + child.y
                if (x >= bx && x <= bx + child.width &&
                    y >= by && y <= by + child.height) {
                    return child
                }
            }
        }
        return null
    }
}
