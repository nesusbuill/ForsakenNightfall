package com.studios.forsaken2d

import android.content.Intent
import android.os.Bundle
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
        setContentView(R.layout.activity_lobby)

        val tvIp = findViewById<TextView>(R.id.tvIp)
        val etName = findViewById<EditText>(R.id.etName)
        val etIp = findViewById<EditText>(R.id.etIp)
        tvIp.text = "IP: ${getLocalIp()}"

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
            Toast.makeText(this, "Room created! IP: ${getLocalIp()}", Toast.LENGTH_LONG).show()
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
    }

    private fun getLocalIp(): String {
        try {
            val ifs = NetworkInterface.getNetworkInterfaces()
            while (ifs.hasMoreElements()) {
                val i = ifs.nextElement()
                val addrs = i.inetAddresses
                while (addrs.hasMoreElements()) {
                    val a = addrs.nextElement()
                    if (!a.isLoopbackAddress && a is Inet4Address) return a.hostAddress ?: "?"
                }
            }
        } catch (_: Exception) {}
        return "Unknown"
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!NetworkManager.isHost) NetworkManager.disconnect()
    }
}
