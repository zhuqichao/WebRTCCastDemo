package com.herman.castreceiver.main

import com.herman.castreceiver.bean.Candidate
import com.herman.castreceiver.bean.CastClose
import com.herman.castreceiver.bean.Desc

/**
 * @author Herman.
 * @time 2021/8/17
 */
interface OnCastMessageListener {
  fun onCastClosed(castClose: CastClose)
  fun onCastDesc(desc: Desc)
  fun onCastCandidate(candidate: Candidate)
}