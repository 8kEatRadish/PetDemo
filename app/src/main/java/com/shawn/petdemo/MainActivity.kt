package com.shawn.petdemo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {


    private var contentString: String by Delegates.observable("") { _, _, newValue ->

        GlobalScope.launch(Dispatchers.Main) {
            "${textContent?.text}\n$newValue".also { textContent?.text = it }
        }
    }

    private var textContent: TextView? = null

    private var isServer = true


    private var serviceConnect: ServiceConnection? = null

    private var server: (SocketServer.SocketBinder)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textContent = findViewById(R.id.content)

        findViewById<Button>(R.id.start_server).setOnClickListener {
            isServer = true
            contentString = "本机ip = ${getLocalIPAddress(this)}"
            SocketServer.callBack = {
                contentString = it
            }

            startService(Intent(this, SocketServer::class.java))

            serviceConnect = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    this@MainActivity.server = service as SocketServer.SocketBinder
                }

                override fun onServiceDisconnected(name: ComponentName?) {}
            }

            serviceConnect?.apply {
                this@MainActivity.bindService(Intent(this@MainActivity, SocketServer::class.java),
                    this,
                    Context.BIND_AUTO_CREATE)
            }

            findViewById<Button>(R.id.start_server).visibility = View.GONE
            findViewById<Button>(R.id.connect).visibility = View.GONE
            findViewById<EditText>(R.id.host).visibility = View.GONE
        }

        findViewById<Button>(R.id.connect).setOnClickListener {
            isServer = false
            SocketClient.createClient(findViewById<EditText>(R.id.host).text.toString(), 1234) {
                if (it == "tcp socket client :: connect server successful") {
                    GlobalScope.launch(Dispatchers.Main) {
                        findViewById<Button>(R.id.start_server).visibility = View.GONE
                        findViewById<Button>(R.id.connect).visibility = View.GONE
                        findViewById<EditText>(R.id.host).visibility = View.GONE
                    }
                }
                contentString = it
            }
        }

        findViewById<Button>(R.id.send).setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                if (isServer) {
                    server?.sendMsg(findViewById<EditText>(R.id.data).text.toString())
                } else {
                    SocketClient.sendData(findViewById<EditText>(R.id.data).text.toString())
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnect?.apply {
            unbindService(this)
        }
    }
}