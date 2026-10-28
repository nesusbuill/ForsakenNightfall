package com.studios.forsaken2d

object LMSManager {
    data class LMSResult(val duration: Float, val themeName: String, val musicResId: Int = 0)

    fun getConfig(killerId: String, killerSkin: String, survivorId: String, survivorSkin: String): LMSResult {
        if (setOf(killerId, survivorId) == setOf("c00lkidd", "007n7"))
            return LMSResult(90f, "PLEAD")
        if (setOf(killerId, survivorId) == setOf("1x1x1x1", "shedletsky"))
            return LMSResult(90f, "CREATION OF HATRED")
        if (setOf(killerId, survivorId) == setOf("guest666", "noob"))
            return LMSResult(90f, "DEAD RINGER")
        if (setOf(killerId, survivorId) == setOf("johndoe", "janedoe"))
            return LMSResult(100f, "You and I")

        if (killerId == "1x1x1x1" && killerSkin == "Diva" && survivorId == "twotime" && survivorSkin == "Ghoul")
            return LMSResult(90f, "BURNOUT")
        if (killerId == "1x1x1x1" && killerSkin == "Hacklord" && survivorId == "shedletsky" && survivorSkin == "Heartbroken")
            return LMSResult(90f, "Through Patches of Violet")
        if (killerId == "1x1x1x1" && killerSkin == "Crazyblox" && survivorId == "shedletsky" && survivorSkin == "The Guide")
            return LMSResult(90f, "FOR OLD TIMES SAKE")
        if (killerId == "johndoe" && killerSkin == "Annihilation" && survivorId == "elliot" && survivorSkin == "Friend")
            return LMSResult(90f, "close to me")
        if (killerId == "noli" && killerSkin == "The Possessed" && survivorId == "guest1337" && survivorSkin == "Guard")
            return LMSResult(90f, "The Way It Ends")
        if (killerId == "c00lkidd" && killerSkin == "PartyP4wner" && survivorId == "elliot")
            return LMSResult(90f, "Make A Cake")
        if (killerId == "c00lkidd" && killerSkin == "Bluudud" && survivorId == "007n7" && survivorSkin == "118o8")
            return LMSResult(90f, "MADE JUST 4 U")
        if (killerId == "azure" && survivorId == "twotime")
            return LMSResult(90f, "And The Seraphim Wept")

        if (killerId == "slasher" && killerSkin == "Vanity")
            return LMSResult(70f, "Killer Vanity")
        if (killerId == "johndoe" && killerSkin == "Gasharpoon")
            return LMSResult(70f, "Curiosity")
        if (survivorSkin == "Golden" &&
            (survivorId == "shedletsky" || survivorId == "chance" || survivorId == "dusekkar"))
            return LMSResult(70f, "Profitable Passion")
        if ((killerSkin == "Classic" || killerSkin == "Retro") &&
            (survivorSkin == "Classic" || survivorSkin == "Retro"))
            return LMSResult(70f, "Escape The Necrobloxians!")

        return LMSResult(70f, "A GRAVE SOUL (NOW, RUN)")
    }
}
