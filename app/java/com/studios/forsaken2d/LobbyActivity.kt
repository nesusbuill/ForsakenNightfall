package com.studios.forsaken2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface

class LobbyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("FORSAKEN_LOBBY", "onCreate started")

        try {
            // 1. Ép xoay ngang SAU KHI đã setContentView (tránh treo)
            // 2. Full-screen immersive
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )

            setContentView(R.layout.activity_lobby)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

            val tvIp = findViewById<TextView>(R.id.tvIp)
            val etName = findViewById<EditText>(R.id.etName)
            val etIp = findViewById<EditText>(R.id.etIp)
            
            // Hiển thị text chờ trước
            tvIp.text = "IP: Đang lấy..." 

            // 🔥 FIX TREO: Lấy IP ở Background Thread
            Thread {
                val ip = getLocalIp()
                runOnUiThread {
                    tvIp.text = "IP: $ip"
                }
            }.start()

            NetworkManager.onGameStart = {
                runOnUiThread {
                    startActivity(Intent(this, GameActivity::class.java))
                    finish()
                }
            }

            findViewById<Button>(R.id.btnSinglePlayer).setOnClickListener {
                NetworkManager.playerName = etName.text.toString().ifEmpty { "Player" }
                NetworkManager.isHost = true
                NetworkManager.isConnected = false
                startActivity(Intent(this, GameActivity::class.java))
                finish()
            }

            findViewById<Button>(R.id.btnCreateRoom).setOnClickListener {
                NetworkManager.playerName = etName.text.toString().ifEmpty { "Host" }
                NetworkManager.host()
                Toast.makeText(this, "Room created! IP: ${tvIp.text.toString().replace("IP: ", "")}", Toast.LENGTH_LONG).show()
            }

            findViewById<Button>(R.id.btnJoin).setOnClickListener {
                val ip = etIp.text.toString()
                if (ip.isEmpty()) {
                    Toast.makeText(this, "Enter IP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                NetworkManager.playerName = etName.text.toString().ifEmpty { "Player" }
                NetworkManager.join(ip)
                Toast.makeText(this, "Connecting...", Toast.LENGTH_SHORT).show()
            }

            Log.d("FORSAKEN_LOBBY", "onCreate finished successfully")

        } catch (e: Exception) {
            Log.e("FORSAKEN_LOBBY", "onCreate crashed", e)
            Toast.makeText(this, "Lỗi khởi động: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    // Hàm này giờ chỉ chạy ở Thread phụ nên không làm treo app nữa
    private fun getLocalIp(): String {
        try {
            val ifs = NetworkInterface.getNetworkInterfaces()
            while (ifs.hasMoreElements()) {
                val i = ifs.nextElement()
                val addrs = i.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is Inet4Address) {
                        return a.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FORSAKEN_LOBBY", "Lỗi lấy IP: ${e.message}")
        }
        return "Không có mạng"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!NetworkManager.isHost) NetworkManager.disconnect()
    }
}
