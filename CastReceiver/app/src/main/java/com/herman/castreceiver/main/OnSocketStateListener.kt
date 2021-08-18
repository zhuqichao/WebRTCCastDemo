package com.herman.castreceiver.main

import com.herman.castreceiver.bean.SocketState

/**
 * @author Herman.
 * @time 2021/8/17
 */
interface OnSocketStateListener {
  fun onState(state: SocketState)
}