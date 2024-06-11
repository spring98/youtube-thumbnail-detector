package com.spring.youtube.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class HostingService : Service() {
    private val CHANNELID = "Foreground Service ID"
    private var clientAddress = ""
    private var clientPort = 0
    private lateinit var notificationManager: NotificationManager


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        thread {
            startServer()
        }

        createNotificationChannel()
        startForeground(888, createNotification("서비스가 실행중입니다."))

        return START_STICKY
    }

    private fun startServer() {
        val port = 1234
        val server = ServerSocket(port)
        Log.e("Service", "Server started on port $port")

        while (true) {
            val client: Socket = server.accept()
            handleClient(client)
        }
    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            val output = socket.getOutputStream()

            clientAddress = socket.inetAddress.hostAddress ?: ""
            clientPort = socket.port

            updateNotification("유저 접속: $clientAddress:$clientPort")
            Log.e("Service", "Client connected: $clientAddress:$clientPort")

            // HTTP Response
            val response = """
                HTTP/1.1 200 OK
                Content-Type: text/html
                Connection: close

                <html>
                <body>
                <h1>Hello, World!</h1>
                <p>Client IP: $clientAddress</p>
                <p>Client Port: $clientPort</p>
                </body>
                </html>
            """.trimIndent()

            output.write(response.toByteArray())
            output.flush()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNELID,
            "Foreground Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        return Notification.Builder(this, CHANNELID)
            .setContentTitle("Youtube Server Hosting")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setStyle(Notification.BigTextStyle().bigText("이것은 서비스가 실행 중임을 나타내는 긴 텍스트입니다."))
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        notificationManager.notify(888, notification)
    }

}