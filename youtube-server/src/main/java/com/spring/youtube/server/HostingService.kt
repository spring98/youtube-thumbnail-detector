@file:Suppress("PrivatePropertyName")

package com.spring.youtube.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import kotlin.concurrent.thread

class HostingService : Service() {
    private val CHANNELID = "Foreground Service ID"
    private var clientAddress = ""
    private var clientPort = 0
    private lateinit var notificationManager: NotificationManager
    private val BROADCAST_PORT = 12345
    private val SERVER_PORT = 1234

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        thread {
            startServer()
        }

        thread {
            startBroadcast()
        }

        createNotificationChannel()
        startForeground(888, createNotification("서비스가 실행중입니다."))

        return START_STICKY
    }

    private fun startServer() {
        val server = ServerSocket(SERVER_PORT)
        Log.e("Service", "Server started on port $SERVER_PORT")

        while (true) {
            val client: Socket = server.accept()
            handleClient(client)
        }
    }

//    private fun handleClient(client: Socket) {
//        client.use { socket ->
//            val output = socket.getOutputStream()
//
//            clientAddress = socket.inetAddress.hostAddress ?: ""
//            clientPort = socket.port
//
//            updateNotification("유저 접속: $clientAddress:$clientPort")
//            Log.e("Service", "Client connected: $clientAddress:$clientPort")
//
//            // HTTP Response
//            val response = """
//                HTTP/1.1 200 OK
//                Content-Type: text/html
//                Connection: close
//
//                <html>
//                <body>
//                <h1>Hello, World!</h1>
//                <p>Client IP: $clientAddress</p>
//                <p>Client Port: $clientPort</p>
//                </body>
//                </html>
//            """.trimIndent()
//
//            output.write(response.toByteArray())
//            output.flush()
//        }
//    }
    private fun handleClient(client: Socket) {
        client.use { socket ->
            val output = socket.getOutputStream()
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input))

            clientAddress = socket.inetAddress.hostAddress ?: ""
            clientPort = socket.port

            updateNotification("유저 접속: $clientAddress:$clientPort")
            Log.e("Service", "Client connected: $clientAddress:$clientPort")

            try {
                // Read the request
                val requestLine = reader.readLine()
                Log.e("Service", "Received request: $requestLine")

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
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                input.close()
                output.close()
                socket.close()
            }
        }
    }

    private fun startBroadcast() {
        val broadcastAddress = InetAddress.getByName("255.255.255.255")
        val socket = DatagramSocket()
        val ipAddress = getLocalIpAddress()

        while (true) {
            try {
                val message = "SERVER_IP:$ipAddress"
                val buffer = message.toByteArray()
                val packet = DatagramPacket(buffer, buffer.size, broadcastAddress, BROADCAST_PORT)
                socket.send(packet)
                Log.e("Service", "Broadcast message sent: $message")
                Thread.sleep(5000)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addresses = intf.inetAddresses
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress ?: ""
                    }
                }
            }
            "Unknown IP"
        } catch (e: Exception) {
            "Unknown IP"
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