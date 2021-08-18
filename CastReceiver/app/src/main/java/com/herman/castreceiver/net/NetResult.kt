package com.herman.castreceiver.net

/**
 * @author Herman.
 * @time 2021/8/17
 */
interface NetResult<T> {
  fun onResult(result: T?, code: Int, message: String)
}