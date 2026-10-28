package com.studios.forsaken2d

import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_game)
        val container = findViewById<FrameLayout>(android.R.id.content)
        container.addView(GameView(this))
    }
}
