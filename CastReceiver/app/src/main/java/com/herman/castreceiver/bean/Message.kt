package com.herman.castreceiver.bean

import com.google.gson.JsonObject
import org.webrtc.SessionDescription

/**
 * @author Herman.
 * @time 2021/8/17
 */
data class MessageHead(
  var from: String, // 消息来自于
  var to: String, // 消息需要发给谁
  var type: MessageType, //消息类型
)

data class Message(
  var head: MessageHead,
  var body: JsonObject,
)

data class MessageSend<T>(
  var head: MessageHead,
  var body: T,
)

// 投屏请求消息
data class Offer(
  var confirm: Boolean
)

data class CastClose(
  var message: String
)

// sdp协商信息
data class Desc(
  var type: String,
  var sdp: String,
)

// Candidate信息
data class Candidate(
  var sdpMLineIndex: Int,
  var candidate: String,
  var sdpMid: String,
)

enum class MessageType {
  OFFER, //投屏请求
  READY, //接受投屏
  REFUSE, //拒绝投屏
  CLOSE, //关闭投屏
  DESC,
  CANDIDATE,
}

enum class SocketState {
  CONNECTING, CONNECTED, CONNECT_FAIL, CLOSED
}