package com.spring.youtube.thumbnail

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
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

        CoroutineScope(Dispatchers.IO).launch {
            val htmlData = fetchHtmlFromServer()
            runOnUiThread {
                webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        }

//        val button: Button = findViewById(R.id.serverBtn)
//        button.setOnClickListener {
//            GlobalScope.launch(Dispatchers.IO) {
//                startClient()
//            }
//        }
    }

    private fun startClient() {
        val serverIp = Server.url  // 서버 IP 주소
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
        val urlString = "http://${Server.url}:1234"  // URL에 프로토콜 추가
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
}