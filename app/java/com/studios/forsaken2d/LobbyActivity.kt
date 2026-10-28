package com.studios.forsaken2d

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LobbyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FORSAKEN_LOBBY", "Bắt đầu onCreate")

        try {
            setContentView(R.layout.activity_lobby)
            Log.d("FORSAKEN_LOBBY", "Đã load Layout thành công")

            val etName = findViewById<EditText>(R.id.etName)

            findViewById<Button>(R.id.btnSinglePlayer).setOnClickListener {
                NetworkManager.playerName = etName.text.toString().ifEmpty { "Player" }
                NetworkManager.isHost = true
                NetworkManager.isConnected = false
                startActivity(Intent(this, GameActivity::class.java))
            }

            findViewById<Button>(R.id.btnCreateRoom).setOnClickListener {
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            }

            findViewById<Button>(R.id.btnJoin).setOnClickListener {
                Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("FORSAKEN_LOBBY", "CRASH Ở LOBBY", e)
            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
