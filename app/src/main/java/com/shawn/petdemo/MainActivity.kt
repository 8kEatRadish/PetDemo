package com.shawn.petdemo

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
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

    private lateinit var startServer: Button
    private lateinit var connect: Button
    private lateinit var send: Button
    private lateinit var host: EditText

    private lateinit var pet: View

    private var animatorSet: AnimatorSet? = null

    private var initY by Delegates.notNull<Float>()
    private var initTop by Delegates.notNull<Float>()
    private var initBottom by Delegates.notNull<Float>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textContent = findViewById(R.id.content)

        pet = findViewById(R.id.pet)
        pet.post {
            initY = pet.y
            initTop = pet.top.toFloat()
            initBottom = pet.bottom.toFloat()
        }

        host = findViewById(R.id.my_host)

        initButton()
    }

    @SuppressLint("CutPasteId")
    private fun initButton() {
        startServer = findViewById<Button>(R.id.start_server).apply {
            setOnClickListener {
                isServer = true
                contentString = "本机ip = ${getLocalIPAddress(this@MainActivity)}"
                SocketServer.callBack = {
                    contentString = it
                }
                pet.y = pet.bottom.toFloat() + pet.y

                pet.visibility = View.GONE

                SocketServer.commandCallBack = {
                    pet.enter(it.commandValue)
                }

                startService(Intent(this@MainActivity, SocketServer::class.java))

                serviceConnect = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        this@MainActivity.server = service as SocketServer.SocketBinder
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}
                }

                serviceConnect?.apply {
                    this@MainActivity.bindService(Intent(this@MainActivity,
                        SocketServer::class.java),
                        this,
                        Context.BIND_AUTO_CREATE)
                }

                startServer.visibility = View.GONE
                connect.visibility = View.GONE
                send.visibility = View.GONE
                host.visibility = View.GONE
            }
        }

        connect = findViewById<Button>(R.id.connect).apply {
            setOnClickListener {
                isServer = false
                SocketClient.createClient(host.text.toString(),
                    1234) {
                    if (it == "tcp socket client :: connect server successful") {
                        GlobalScope.launch(Dispatchers.Main) {
                            startServer.visibility = View.GONE
                            connect.visibility = View.GONE
                            host.visibility = View.GONE
                            send.visibility = View.VISIBLE
                            send.text = "去电视"
                        }
                    }
                    contentString = it
                }
            }
        }

        send = findViewById<Button>(R.id.send).apply {
            setOnClickListener {
                GlobalScope.launch(Dispatchers.IO) {
                    if (isServer) {

                    } else {

                    }
                }
            }
        }

        send.setOnClickListener {
            if (!isServer) {
                Toast.makeText(this, "我要去电视了", Toast.LENGTH_SHORT).show()
                pet.out()
            }
        }
    }

    private fun View.out() {

        animatorSet?.apply {
            if (isRunning) {
                return
            }
        }
        if (this.visibility == View.GONE) {
            this.visibility = View.VISIBLE
        }

        // 弹跳动画
        val jumpAnimator = ObjectAnimator.ofFloat(this, "translationY", 0f, -110f, 0f).apply {
            duration = 1000
            interpolator = BounceInterpolator()
        }

        // 移动到边界

        val moveToSideAnimator =
            ObjectAnimator.ofFloat(this, "y", initY, initY - initTop).apply {
                duration = 1000
                interpolator = LinearInterpolator()
            }

        // 移动到屏幕外
        val valueAnimator = ValueAnimator.ofInt(0, 100).apply {
            addUpdateListener {
                val value = it.animatedValue as Int
                Log.e("suihw", "value = $value")
                GlobalScope.launch(Dispatchers.IO) {
                    SocketClient.sendData(Gson().toJson(CommandBody(0, value)))
                }
                this@out.y = -(this@out.height * value / 100).toFloat()
            }
            duration = 1000
            interpolator = LinearInterpolator()
        }

        animatorSet = AnimatorSet()
        animatorSet?.playSequentially(jumpAnimator, moveToSideAnimator, valueAnimator)
        animatorSet?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                pet.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator?) {

            }

            override fun onAnimationRepeat(animation: Animator?) {

            }

        })
        animatorSet?.start()
    }

    private fun View.enter(process: Int) {
        runOnUiThread {
            this.y = initBottom + initY - this.height * process / 100
            Log.e("suihw", "y = ${this.y} ; initY = $initY")
            if (this.visibility == View.GONE) {
                this.visibility = View.VISIBLE
            }
            if (process == 100) {
                AnimatorSet().apply {
                    playSequentially(
                        ObjectAnimator.ofFloat(this@enter,
                            "y",
                            initBottom + initY - this@enter.height,
                            initY).apply {
                            duration = 1000
                            interpolator = LinearInterpolator()
                        }, ObjectAnimator.ofFloat(this@enter, "translationY", 0f, -110f, 0f).apply {
                            duration = 1000
                            interpolator = BounceInterpolator()
                            start()
                        })
                    start()

                    addListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator?) {

                        }

                        override fun onAnimationEnd(animation: Animator?) {
                            Toast.makeText(this@MainActivity, "我到啦", Toast.LENGTH_SHORT).show()
                        }

                        override fun onAnimationCancel(animation: Animator?) {

                        }

                        override fun onAnimationRepeat(animation: Animator?) {

                        }

                    })
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