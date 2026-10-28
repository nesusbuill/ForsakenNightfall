package com.studios.forsaken2d

class MatchManager {
    var timeLeft = 160f
    var isLMS = false
    var isGameOver = false
    var malice = 0
    var generatorsDone = 0
    val totalGenerators = 5
    var currentLMSTheme = ""

    fun update(dt: Float, aliveSurvivors: Int, killerId: String, killerSkin: String,
               survivorId: String, survivorSkin: String): String? {
        if (isGameOver) return null
        if (aliveSurvivors == 1 && !isLMS) {
            isLMS = true
            val cfg = LMSManager.getConfig(killerId, killerSkin, survivorId, survivorSkin)
            timeLeft = cfg.duration
            currentLMSTheme = cfg.themeName
            return "LMS_TRIGGERED"
        }
        timeLeft -= dt
        if (timeLeft <= 0) {
            timeLeft = 0f
            isGameOver = true
            return "SURVIVOR_WIN"
        }
        return null
    }

    fun onGeneratorDone() { generatorsDone++; malice++; timeLeft -= 2f }
    fun onSurvivorKilled() { timeLeft += 35f }

    fun formattedTime(): String {
        val m = (timeLeft / 60).toInt()
        val s = (timeLeft % 60).toInt()
        return String.format("%d:%02d", m, s)
    }
}
