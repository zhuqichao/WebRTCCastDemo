package com.herman.castservice.service

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.websocket.*
import javax.websocket.server.PathParam
import javax.websocket.server.ServerEndpoint

val webSocketSet = ConcurrentHashMap<String, WebSocket>()

@Component
@ServerEndpoint(value = "/websocket/{id}/{pingCode}/{device}")
class WebSocket {
    private var session: Session? = null
    private var id: String = ""
    private var pingCode: String = ""
    private var device: String = ""

    @OnOpen
    fun onOpen(session: Session,
               @PathParam(value = "id") id: String,
               @PathParam(value = "pingCode") pingCode: String,
               @PathParam(value = "device") device: String) {
        this.session = session
        this.id = id
        this.pingCode = pingCode
        this.device = device
        webSocketSet[id] = this
        println("[$id] 连接成功，当前连接人数为：${webSocketSet.size}")
    }

    @OnClose
    fun onClose() {
        webSocketSet.remove(this.id)
        println("[$id] 断开，当前连接人数为：${webSocketSet.size}")
    }

    @OnMessage
    fun onMessage(message: String) {
        println("[$id] 发来消息：$message")
        // 转发消息到目标接收者
        webSocketSet.values.filter { it.pingCode == pingCode && it.id != id }.forEach {
            it.session?.basicRemote?.sendText(message)
        }
    }

    @OnError
    fun onError(session: Session, error: Throwable) {
        error.printStackTrace()
    }
}