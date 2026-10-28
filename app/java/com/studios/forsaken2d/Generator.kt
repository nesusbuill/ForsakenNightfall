package com.studios.forsaken2d

class Generator(val id: Int, val x: Float, val y: Float, val radius: Float = 40f) {
    var progress = 0f
    var isCompleted = false
    var isBeingRepaired = false
    var knobX = 0f
    val REPAIR_TIME = 160f
    val TRACK_WIDTH = 200f

    fun repair(dt: Float, match: MatchManager): Boolean {
        if (isCompleted) return false
        progress += (dt / REPAIR_TIME) * 100f
        if (progress >= 100f) {
            progress = 100f
            isCompleted = true
            isBeingRepaired = false
            match.onGeneratorDone()
            return true
        }
        return false
    }

    fun resetDrag() { knobX = 0f; isBeingRepaired = false }
}
