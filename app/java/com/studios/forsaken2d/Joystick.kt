package com.studios.forsaken2d

class Joystick(var centerX: Float, var centerY: Float, val baseRadius: Float = 120f) {
    var knobX = centerX
    var knobY = centerY
    var isActive = false

    fun getInput(): Pair<Float, Float> {
        if (!isActive) return 0f to 0f
        return ((knobX - centerX) / baseRadius).coerceIn(-1f, 1f) to
               ((knobY - centerY) / baseRadius).coerceIn(-1f, 1f)
    }

    fun reset() { isActive = false; knobX = centerX; knobY = centerY }
}
