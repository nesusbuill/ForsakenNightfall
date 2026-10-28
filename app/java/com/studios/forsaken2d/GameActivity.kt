package com.studios.forsaken2d

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            // 🔥 Ép xoay ngang
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            // 🔥 Giữ màn hình sáng
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

            // 🔥 Full-screen immersive
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )

            setContentView(R.layout.activity_game)

            val container = findViewById<FrameLayout>(R.id.gameContainer)
            gameView = GameView(this)
            container.addView(gameView)

            Log.d("FORSAKEN_GAME", "Game started in landscape mode")

        } catch (e: Exception) {
            Log.e("FORSAKEN_GAME", "onCreate crashed", e)
            Toast.makeText(this, "Lỗi khởi động game: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        gameView?.resume()
    }

    override fun onPause() {
        super.onPause()
        gameView?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView?.stop()
        gameView = null
    }
}
