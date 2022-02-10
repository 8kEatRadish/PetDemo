package com.shawn.petdemo

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.text.TextUtils
import com.google.gson.Gson
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class SocketServer : Service() {

    var serverAllowed: Boolean = true

    var socket: Socket? = null

    private var runnable = Runnable {
        val serverSocket = ServerSocket(1234)
        callBack?.invoke("开启服务")
        val accept: Socket = serverSocket.accept()
        socket = accept
        Thread { response(accept) }.start()
    }

    companion object {
        var callBack: ((String) -> Unit)? = null
        var commandCallBack: ((CommandBody) -> Unit)? = null
    }

    override fun onCreate() {
        super.onCreate()
        Thread(runnable).start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return SocketBinder()
    }

    inner class SocketBinder : Binder() {
        fun sendMsg(msg: String) = sendData(msg)
    }

    private fun response(accept: Socket) {
        try {
            //从客户端接收的信息
            val bufferedReaderIn = BufferedReader(InputStreamReader(accept.getInputStream()))
            //发送信息给客户端
            val out =
                PrintWriter(BufferedWriter(OutputStreamWriter(accept.getOutputStream())), true)
            while (serverAllowed) {
                val msg = bufferedReaderIn.readLine()
                if (TextUtils.isEmpty(msg)) {
                    callBack?.invoke("收到客户端的信息为空，断开连接")
                    break
                }
                callBack?.invoke("Client Msg： $msg")

                commandCallBack?.invoke(Gson().fromJson(msg, CommandBody::class.java))
//                val msgOp = "加工从客户端的信息： $msg"
//                out.println(msgOp);
            }
            callBack?.invoke("关闭服务")
            bufferedReaderIn.close()
            out.close()
            accept.close()

        } catch (e: Exception) {
            println(e.message)
        }

    }

    fun sendData(data: String) {

        if (socket == null || socket!!.isClosed) {
            callBack?.invoke("链接断开")
            serverAllowed = false
            return
        }

        try {
            val writer =
                PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())),
                    true)
            println("suihw 服务端发送出消息 $data")
            writer.println("$data;${System.currentTimeMillis()}")
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        serverAllowed = false
    }
}