package com.studios.forsaken2d

import android.util.Log
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

object NetworkManager {
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private val clients = CopyOnWriteArrayList<ClientHandler>()
    var isHost = false
    var playerName = "Player"
    var isConnected = false

    var onPlayerList: ((List<String>) -> Unit)? = null
    var onGameStart: (() -> Unit)? = null
    var onPosReceived: ((String, Float, Float) -> Unit)? = null
    var onDisconnect: (() -> Unit)? = null

    fun host(port: Int = 8888) {
        isHost = true
        isConnected = true
        Thread {
            try {
                serverSocket = ServerSocket(port)
                onPlayerList?.invoke(listOf(playerName))
                while (serverSocket?.isClosed == false) {
                    val s = serverSocket!!.accept()
                    val h = ClientHandler(s)
                    clients.add(h)
                    Thread(h).start()
                }
            } catch (e: Exception) { Log.e("Net", "host: ${e.message}") }
        }.start()
    }

    fun join(ip: String, port: Int = 8888) {
        isHost = false
        Thread {
            try {
                clientSocket = Socket(ip, port)
                PrintWriter(clientSocket!!.getOutputStream(), true).println("JOIN|$playerName")
                val r = BufferedReader(InputStreamReader(clientSocket!!.getInputStream()))
                isConnected = true
                var line: String?
                while (r.readLine().also { line = it } != null) handle(line!!, null)
            } catch (e: Exception) {
                isConnected = false
                onDisconnect?.invoke()
            }
        }.start()
    }

    fun sendPos(x: Float, y: Float) {
        val msg = "POS|$playerName|$x|$y"
        if (isHost) broadcast(msg)
        else try { PrintWriter(clientSocket?.getOutputStream(), true)?.println(msg) } catch (_: Exception) {}
    }

    private fun broadcast(msg: String) {
        for (c in clients) c.send(msg)
        handle(msg, null)
    }

    private fun handle(msg: String, sender: ClientHandler?) {
        val p = msg.split("|")
        when (p.getOrNull(0)) {
            "JOIN" -> sender?.let { it.name = p.getOrNull(1) ?: "Unknown"; notifyHostList() }
            "UPDATE_LIST" -> {
                val arr = JSONArray(p.getOrNull(1) ?: "[]")
                onPlayerList?.invoke((0 until arr.length()).map { arr.getString(it) })
            }
            "START" -> onGameStart?.invoke()
            "POS" -> {
                val n = p.getOrNull(1) ?: ""
                val x = p.getOrNull(2)?.toFloatOrNull() ?: 0f
                val y = p.getOrNull(3)?.toFloatOrNull() ?: 0f
                if (isHost && sender != null) broadcast("POS|$n|$x|$y")
                if (n != playerName) onPosReceived?.invoke(n, x, y)
            }
        }
    }

    private fun notifyHostList() {
        val list = mutableListOf(playerName)
        for (c in clients) if (c.name.isNotEmpty()) list.add(c.name)
        broadcast("UPDATE_LIST|${JSONArray(list)}")
        onPlayerList?.invoke(list)
    }

    fun disconnect() {
        try { serverSocket?.close() } catch (_: Exception) {}
        try { clientSocket?.close() } catch (_: Exception) {}
        for (c in clients) c.close()
        clients.clear()
        isConnected = false
    }

    inner class ClientHandler(private val socket: Socket) : Runnable {
        var name = ""
        private var out: PrintWriter? = null
        override fun run() {
            try {
                out = PrintWriter(socket.getOutputStream(), true)
                val r = BufferedReader(InputStreamReader(socket.getInputStream()))
                var line: String?
                while (r.readLine().also { line = it } != null) handle(line!!, this)
            } catch (_: Exception) {} finally {
                clients.remove(this); notifyHostList(); close()
            }
        }
        fun send(msg: String) { out?.println(msg) }
        fun close() { try { socket.close() } catch (_: Exception) {} }
    }
}
