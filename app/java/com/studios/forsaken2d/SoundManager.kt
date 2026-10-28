package com.studios.forsaken2d

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

object SoundManager {
    private var bgmPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<Int, Int>()

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(5).setAudioAttributes(audioAttributes).build()
    }

    fun playBGM(context: Context, resId: Int, loop: Boolean = true) {
        stopBGM()
        if (resId == 0) return
        try {
            bgmPlayer = MediaPlayer.create(context, resId).apply {
                isLooping = loop
                setVolume(0.5f, 0.5f)
                start()
            }
        } catch (_: Exception) {}
    }

    fun stopBGM() {
        bgmPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        bgmPlayer = null
    }

    fun playSFX(resId: Int) {
        if (resId == 0) return
        soundPool?.let { pool ->
            soundMap[resId]?.let { soundId ->
                pool.play(soundId, 1f, 1f, 0, 0, 1f)
            }
        }
    }

    fun release() {
        stopBGM()
        soundPool?.release()
        soundPool = null
        soundMap.clear()
    }
}
