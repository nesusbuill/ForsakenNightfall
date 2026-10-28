package com.studios.forsaken2d

class Skill(val name: String, val maxCooldown: Float, val color: Int) {
    var cooldown: Float = 0f
        private set

    fun update(dt: Float) {
        if (cooldown > 0) cooldown = (cooldown - dt).coerceAtLeast(0f)
    }

    fun trigger(): Boolean {
        if (cooldown <= 0f) { cooldown = maxCooldown; return true }
        return false
    }
}
