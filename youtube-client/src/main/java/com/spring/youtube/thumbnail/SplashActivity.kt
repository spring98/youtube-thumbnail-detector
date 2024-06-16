package com.spring.youtube.thumbnail

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.spring.youtube.thumbnail.utils.Server
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket

class SplashActivity : AppCompatActivity() {
    private val tag = "spring-main"
    private val BROADCAST_PORT = 12345

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Start receiving broadcast messages
        GlobalScope.launch(Dispatchers.IO) {
            receiveBroadcastMessages()
        }
    }

    private fun receiveBroadcastMessages() {
        val socket = DatagramSocket(BROADCAST_PORT)
        val buffer = ByteArray(1024)
        val packet = DatagramPacket(buffer, buffer.size)

        while (true) {
            socket.receive(packet)
            val message = String(packet.data, 0, packet.length)
            if (message.startsWith("SERVER_IP:")) {
                val serverIp = message.substringAfter("SERVER_IP:")
                Log.e(tag, "Server IP: $serverIp")
                Server.url = serverIp
                break
            }
        }

        startActivity(Intent(this, HomeActivity::class.java))
    }
}