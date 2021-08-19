package com.herman.castreceiver.net

/**
 * @author Herman.
 * @time 2021/8/17
 */
object Const {

  const val EXTRA_KEY_CAST_OFFER_ID = "extra_key_cast_offer_id"

  const val HOST = "10.4.152.66:8080"
  const val BASE_URL_CAST = "http://$HOST/cast/"
  const val BASE_URL_WEBSOCKET = "ws://$HOST/websocket/"

  const val URI_STUN1 = "stun:stun.ekiga.net"
  const val URI_STUN2 = "stun:stun.fwdnet.net"
}