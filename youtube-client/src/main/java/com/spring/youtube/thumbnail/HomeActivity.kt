package com.spring.youtube.thumbnail

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import com.google.gson.Gson
import com.spring.youtube.thumbnail.utils.Server
import com.spring.youtube.thumbnail.utils.Server.Companion.url
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

@OptIn(DelicateCoroutinesApi::class)
class HomeActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true

//        CoroutineScope(Dispatchers.IO).launch {
//            val htmlData = fetchHtmlFromServer()
//            runOnUiThread {
//                webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
//            }
//        }

        val button: Button = findViewById(R.id.serverBtn)
        val jsonButton: Button = findViewById(R.id.jsonBtn)
        val imageButton: Button = findViewById(R.id.imageBtn)
        val videoButton: Button = findViewById(R.id.videoBtn)

        button.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                startClient()
            }
        }

        jsonButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                fetchJsonFromServer()
            }
        }

        imageButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                fetchImageFromServer()
            }
        }

        videoButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                fetchVideoFromServer()
            }
        }
    }

    private fun startClient() {
        val serverIp = url  // 서버 IP 주소
        val serverPort = 1234  // 서버 포트

        try {
            val socket = Socket(serverIp, serverPort)
            println("Connected to server: $serverIp:$serverPort")

            // 서버에 요청 보내기
            val output = OutputStreamWriter(socket.getOutputStream())
            output.write("GET / HTTP/1.1\r\n")
            output.write("Host: $serverIp\r\n")
            output.write("\r\n")
            output.flush()

            // 서버로부터 응답 받기
            val input = BufferedReader(InputStreamReader(socket.getInputStream()))
            var responseLine: String?

            while (input.readLine().also { responseLine = it } != null) {
                println(responseLine)
            }

            input.close()
            output.close()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fetchHtmlFromServer(): String {
        val urlString = "http://$url:1234"  // URL에 프로토콜 추가
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val stringBuilder = StringBuilder()
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }

            reader.close()
            inputStream.close()
            return stringBuilder.toString()
        } else {
            throw Exception("Failed to fetch HTML: HTTP response code $responseCode")
        }
    }

    private suspend fun fetchJsonFromServer() {
        val urlString = "http://$url:1234/json"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()
                reader.close()
                inputStream.close()

                val responseData = Gson().fromJson(response, ResponseData::class.java)
                withContext(Dispatchers.Main) {
                    webView.loadDataWithBaseURL(null, """
                        <html>
                        <body>
                        <h1>${responseData.message}</h1>
                        <p>Client IP: ${responseData.clientIp}</p>
                        <p>Client Port: ${responseData.clientPort}</p>
                        </body>
                        </html>
                    """.trimIndent(), "text/html", "UTF-8", null)
                }
            } else {
                Log.e("HomeActivity", "Failed to fetch JSON: HTTP response code $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchImageFromServer() {
        val urlString = "http://$url:1234/image"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                Log.d("spring", inputStream.toString())
                val bitmap = BitmapFactory.decodeStream(inputStream)
                Log.d("spring", inputStream.toString())

                inputStream.close()

                Log.d("spring", bitmap.toString())

//                withContext(Dispatchers.Main) {
//                    imageView.setImageBitmap(bitmap)
//                }
            } else {
                Log.e("HomeActivity", "Failed to fetch image: HTTP response code $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchVideoFromServer() {
        val urlString = "http://$url:1234/video"
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val videoBytes = inputStream.readBytes()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    // Here, you can save the video to a file and play it using a VideoView
                    // For simplicity, we will log the size of the video data
                    Log.d("HomeActivity", "Fetched video with size: ${videoBytes.size} bytes")
                }
            } else {
                Log.e("HomeActivity", "Failed to fetch video: HTTP response code $responseCode")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }
}

data class ResponseData(val message: String, val clientIp: String, val clientPort: Int)