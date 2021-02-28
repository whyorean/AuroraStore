package com.aurora.store.view.custom.progress.indicators

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import java.util.*

class BallPulseIndicator : Indicator() {

    private val scaleFloats = floatArrayOf(
        0.75f,
        1.0f,
        0.75f
    )

    override fun draw(canvas: Canvas, paint: Paint) {
        val circleSpacing = 4f
        val radius = (width.coerceAtMost(height) - circleSpacing * 2) / 6
        val x = width / 2f - (radius * 2 + circleSpacing)
        val y = height / 2f

        for (i in 0..2) {
            canvas.save()
            val translateX = x + radius * 2 * i + circleSpacing * i
            canvas.translate(translateX, y)
            canvas.scale(scaleFloats[i], scaleFloats[i])
            canvas.drawCircle(0f, 0f, radius, paint)
            canvas.restore()
        }
    }

    override fun onCreateAnimators(): ArrayList<ValueAnimator> {
        val animators = ArrayList<ValueAnimator>()
        val delays = intArrayOf(120, 240, 360)

        for (i in 0..2) {
            val scaleAnim = ValueAnimator.ofFloat(1f, 0.3f, 1f)
            scaleAnim.duration = 1000
            scaleAnim.repeatCount = -1
            scaleAnim.startDelay = delays[i].toLong()

            addUpdateListener(scaleAnim) {
                scaleFloats[i] = it.animatedValue as Float
                postInvalidate()
            }

            animators.add(scaleAnim)
        }
        return animators
    }
}