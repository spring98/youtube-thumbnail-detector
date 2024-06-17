@file:Suppress("PrivatePropertyName")

package com.spring.youtube.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.*
import kotlin.concurrent.thread

class HostingService : Service() {
    private val tag = "spring-main"
    private val CHANNELID = "Foreground Service ID"
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
        Log.d(tag, "Server started on port $SERVER_PORT")

        while (true) {
            val client: Socket = server.accept()
            handleClient(client)
        }
    }

//    private fun handleClient(client: Socket) {
//        client.use { socket ->
//            val output = socket.getOutputStream()
//            val input = socket.getInputStream()
//            val reader = BufferedReader(InputStreamReader(input))
//
//            val clientAddress = socket.inetAddress.hostAddress ?: ""
//            val clientPort = socket.port
//
//            updateNotification("유저 접속: $clientAddress:$clientPort")
//            Log.d(tag, "Client connected: $clientAddress:$clientPort")
//
//            try {
//                // Read the request
//                val requestLine = reader.readLine()
//                Log.e(tag, "Received request: $requestLine")
//                Log.e(tag, "Server started on port $SERVER_PORT")
//
//                /**
//                 * Header
//                 *  1. General Header
//                 *      1. Date
//                 *      2. Connection
//                 *          close: 메세지 교환 후 TCP 연결 종료
//                 *          Keep-Alive: 메세지 교환 후 TCP 연결 유지
//                 *
//                 *  2. Request Header
//                 *      1. Host
//                 *      2. User-Agent
//                 *      3. Accept
//                 *      4. Cookie
//                 *      5. Referer
//                 *
//                 *  3. Response Header
//                 *      1. Server
//                 *      2. content-encoding
//                 *      3. content-type
//                 *      4. cache-control
//                 *      5. date
//                 *      6. vary
//                 *      7. Set-Cookie
//                 *      8. Age
//                 */
//
////                // HTTP Response
////                val response = """
////                        HTTP/1.1 200 OK
////                        Content-Type: text/html
////                        Connection: close
////
////                        <html>
////                        <body>
////                        <h1>Hello, World!</h1>
////                        <p>Client IP: $clientAddress</p>
////                        <p>Client Port: $clientPort</p>
////                        </body>
////                        </html>
////                    """.trimIndent()
////
////                output.write(response.toByteArray())
////                output.flush()
//
//                // Load image from assets
//                val imageStream: InputStream = assets.open("waffle-preview-image.png")
//                val imageBytes = imageStream.readBytes()
//                imageStream.close()
//
//                // HTTP Response
//                val responseHeader = """
//                HTTP/1.1 200 OK
//                Content-Type: image/png
//                Content-Length: ${imageBytes.size}
//                Connection: close
//
//            """.trimIndent()
//
//                output.write(responseHeader.toByteArray())
//                output.write(imageBytes)
//                output.flush()
//            } catch (e: Exception) {
//                e.printStackTrace()
//            } finally {
//                input.close()
//                output.close()
//                socket.close()
//            }
//        }
//    }

    private fun handleClient(client: Socket) {
        client.use { socket ->
            val output = socket.getOutputStream()
            val input = socket.getInputStream()
            val reader = BufferedReader(InputStreamReader(input))

            val clientAddress = socket.inetAddress.hostAddress ?: ""
            val clientPort = socket.port

            updateNotification("유저 접속: $clientAddress:$clientPort")
            Log.d(tag, "Client connected: $clientAddress:$clientPort")

            try {
                // Read the request
                val requestLine = reader.readLine()
                Log.d(tag, "Received request: $requestLine")

                val requestParts = requestLine.split(" ")
                if (requestParts.size < 2) return

                Log.d(tag, requestParts.toString())

                val method = requestParts[0]
                val path = requestParts[1]

                when (path) {
                    "/json" -> sendJsonResponse(output, clientAddress, clientPort)
                    "/image" -> sendImageResponse(output, "waffle-preview-image.png")
                    "/video" -> sendVideoResponse(output, "waffle-preview-video.mp4")
                    else -> sendNotFoundResponse(output)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                input.close()
                output.close()
                socket.close()
            }
        }
    }

    private fun sendJsonResponse(output: OutputStream, clientAddress: String, clientPort: Int) {
        val responseData = ResponseData("예뻤어.", clientAddress, clientPort)
        val jsonResponse = Gson().toJson(responseData)
        val response = """
            HTTP/1.1 200 OK
            Content-Type: application/json
            Connection: close

            $jsonResponse
        """.trimIndent()

        output.write(response.toByteArray())
        output.flush()
    }

    private fun sendImageResponse(output: OutputStream, imagePath: String) {
        try {
            val imageStream: InputStream = assets.open(imagePath)
            val imageBytes = imageStream.readBytes()
            imageStream.close()

            // 정확한 형식으로 헤더 작성
            val responseHeader = StringBuilder()
            responseHeader.append("HTTP/1.1 200 OK\r\n")
            responseHeader.append("Content-Type: image/png\r\n")
            responseHeader.append("Content-Length: ${imageBytes.size}\r\n")
            responseHeader.append("Connection: close\r\n")
            responseHeader.append("\r\n")

            Log.d(tag, "Response Header: $responseHeader")
            Log.d(tag, "Image Bytes Length: ${imageBytes.size}")

            output.write(responseHeader.toString().toByteArray(Charsets.UTF_8))
            output.write(imageBytes)
            output.flush()
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(tag, "Error sending image response: ${e.message}")
        }
    }

    private fun sendVideoResponse(output: OutputStream, videoPath: String) {
        val videoStream: InputStream = assets.open(videoPath)
        val videoBytes = videoStream.readBytes()
        videoStream.close()

        // 정확한 형식으로 헤더 작성
        val responseHeader = StringBuilder()
        responseHeader.append("HTTP/1.1 200 OK\r\n")
        responseHeader.append("Content-Type: video/mp4\r\n")
        responseHeader.append("Content-Length: ${videoBytes.size}\r\n")
        responseHeader.append("Connection: close\r\n")
        responseHeader.append("\r\n")

        output.write(responseHeader.toString().toByteArray(Charsets.UTF_8))
        output.write(videoBytes)
        output.flush()
    }

    private fun sendNotFoundResponse(output: OutputStream) {
        val response = """
            HTTP/1.1 404 Not Found
            Content-Type: text/html
            Connection: close

            <html>
            <body>
            <h1>404 Not Found</h1>
            </body>
            </html>
        """.trimIndent()

        output.write(response.toByteArray())
        output.flush()
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
                Log.d(tag, "Broadcast message sent: $message")
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

data class ResponseData(val message: String, val clientIp: String, val clientPort: Int)