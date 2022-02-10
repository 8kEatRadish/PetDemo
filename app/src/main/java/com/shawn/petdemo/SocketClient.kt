package com.shawn.petdemo

import kotlinx.coroutines.*
import java.io.*
import java.net.Socket

object SocketClient {

    private var callBack: ((String) -> Unit)? = null

    private var socket: Socket? = null

    fun createClient(host: String, port: Int, callBack: (String) -> Unit) {
        this.callBack = callBack

        GlobalScope.launch(Dispatchers.IO) {
            val result = async { initClient(host, port) }
            result.await()
        }
    }

    private suspend fun initClient(host: String, port: Int) {
        val sc: Socket? = Socket(host, port) //通过socket连接服务器,参数ip为服务端ip地址，port为服务端监听端口
        if (sc != null) {    //判断一下是否连上，避免NullPointException
            callBack?.invoke("tcp socket client :: connect server successful")
            socket = sc
            startReader(socket!!)
        } else {
            callBack?.invoke("tcp socket client :: connect server failed,now retry...")
            initClient(host, port)   //没连上就重试一次
        }
    }

    private suspend fun startReader(socket: Socket) = withContext(Dispatchers.IO) {
        try {
            var reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (true) {
                var msg = reader.readLine()
                callBack?.invoke("Server Msg: ${msg.split(";")[0]}")
//                sendData("加工从服务端的信息： $msg")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendData(data: String) {

        if (socket == null || socket!!.isClosed) {
            callBack?.invoke("tcp socket client :: disconnect server")
            return
        }

        if (data.isEmpty()) {
            callBack?.invoke("发送内容为空")
            return
        }

        try {
            val writer =
                PrintWriter(BufferedWriter(OutputStreamWriter(socket!!.getOutputStream())),
                    true)
            println("suihw 客户端发送出消息 $data")
            writer.println("$data")
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}