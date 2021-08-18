package com.herman.castreceiver.main

import android.app.AlertDialog
import android.app.Dialog
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.herman.castreceiver.bean.*
import com.herman.castreceiver.net.Const
import com.herman.castreceiver.net.NetResult
import com.herman.castreceiver.net.NetWork
import com.herman.castreceiver.utils.DeviceUtils
import io.crossbar.autobahn.websocket.WebSocketConnection
import io.crossbar.autobahn.websocket.WebSocketConnectionHandler
import java.util.*

/**
 * @author Herman.
 * @time 2021/8/16
 */
class CastService : Service() {

  companion object {
    private const val TAG = "CastService"
    val androidId = DeviceUtils.getAndroidId()
    val gson = Gson()

    fun start() {
      CastApplication.get().startService(Intent(CastApplication.get().baseContext, CastService::class.java))
    }
  }

  var socketState = SocketState.CLOSED
  var pingCode = ""
  private var webSocketConnection = WebSocketConnection()
  private var castOfferDialog: Dialog? = null

  private val onSocketStateListeners = mutableListOf<OnSocketStateListener>()
  private val onCastMessageListener = mutableListOf<OnCastMessageListener>()

  override fun onCreate() {
    super.onCreate()
    Log.d(TAG, "onCreate: ANDROID_ID=${androidId}")
    NetWork.initDevice(DeviceInfo(androidId, "", Build.PRODUCT, DeviceType.TV), object : NetResult<DeviceInfo> {
      override fun onResult(result: DeviceInfo?, code: Int, message: String) {
        pingCode = result?.code ?: ""
        createWebSocket(pingCode)
      }
    })
  }

  private fun createWebSocket(pingCode: String) {
    if (webSocketConnection.isConnected) {
      webSocketConnection.sendClose()
    }
    val uri = "${Const.BASE_URL_WEBSOCKET}$androidId/$pingCode/${DeviceType.TV}"
    Log.d(TAG, "createWebSocket: uri=$uri")
    webSocketConnection.connect(uri, webSocketHandler)
    socketState = SocketState.CONNECTING
    callSocketStateChanged(socketState)
  }

  fun <T> sendSocketMessage(message: MessageSend<T>) {
    if (webSocketConnection.isConnected) {
      Log.d(TAG, "sendSocketMessage: $message")
      webSocketConnection.sendMessage(gson.toJson(message))
    }
  }

  fun addOnSocketStateListener(listener: OnSocketStateListener) {
    onSocketStateListeners.add(listener)
  }

  fun removeOnSocketStateListener(listener: OnSocketStateListener) {
    onSocketStateListeners.remove(listener)
  }

  fun addOnCastMessageListener(listener: OnCastMessageListener) {
    onCastMessageListener.add(listener)
  }

  fun removeOnCastMessageListener(listener: OnCastMessageListener) {
    onCastMessageListener.remove(listener)
  }

  private fun callSocketStateChanged(state: SocketState) {
    onSocketStateListeners.forEach {
      it.onState(state)
    }
  }

  private fun callCastClosed(castClose: CastClose) {
    onCastMessageListener.forEach {
      it.onCastClosed(castClose)
    }
  }

  private fun callCaseDesc(desc: Desc) {
    onCastMessageListener.forEach {
      it.onCastDesc(desc)
    }
  }

  private fun callCastCandidate(candidate: Candidate) {
    onCastMessageListener.forEach {
      it.onCastCandidate(candidate)
    }
  }

  private fun showCastOfferDialog(id: String) {
    castOfferDialog?.dismiss()
    val builder = AlertDialog.Builder(CastApplication.get().baseContext).apply {
      setTitle("投屏请求")
      setMessage("是否接受来自" + id + "的投屏请求？")
      setNegativeButton("接受") { _, _ -> startCastActivity(id) }
      setPositiveButton("拒绝") { _, _ -> sendRefuse(id) }
      setOnCancelListener { sendRefuse(id) }
    }
    castOfferDialog = builder.create().apply {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
      } else {
        window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
      }
      show()
    }
  }

  private fun startCastActivity(id: String) {
    val intent = Intent(this, CastActivity::class.java).apply {
      putExtra(Const.EXTRA_KEY_CAST_OFFER_ID, id)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
  }

  private fun sendRefuse(id: String) {
    val head = MessageHead(androidId, id, MessageType.REFUSE)
    sendSocketMessage(MessageSend(head, JsonObject()))
  }

  private val webSocketHandler = object : WebSocketConnectionHandler() {
    override fun onOpen() {
      Log.d(TAG, "onOpen: WebSocket已连接")
      socketState = SocketState.CONNECTED
      callSocketStateChanged(socketState)
    }

    override fun onMessage(message: String) {
      Log.d(TAG, "onMessage: WebSocket消息：$message")
      val msg = gson.fromJson(message, Message::class.java)
      when (msg.head.type) {
        MessageType.OFFER -> {
          val body = gson.fromJson(msg.body, Offer::class.java)
          if (body.confirm) {
            showCastOfferDialog(msg.head.from)
          } else {
            startCastActivity(msg.head.from)
          }
        }
        MessageType.CLOSE -> {
          callCastClosed(gson.fromJson(msg.body, CastClose::class.java))
        }
        MessageType.DESC -> {
          castOfferDialog?.dismiss()
          callCaseDesc(gson.fromJson(msg.body, Desc::class.java))
        }
        MessageType.CANDIDATE -> {
          callCastCandidate(gson.fromJson(msg.body, Candidate::class.java))
        }
        else -> {
        }
      }
    }

    override fun onClose(code: Int, reason: String?) {
      Log.d(TAG, "onClose: WebSocket断开连接：code=$code, reason=$reason")
      socketState = SocketState.CLOSED
      callSocketStateChanged(socketState)
    }
  }

  override fun onBind(intent: Intent?): IBinder {
    return LocalBinder()
  }

  inner class LocalBinder : Binder() {
    fun getService(): CastService {
      return this@CastService
    }
  }

}