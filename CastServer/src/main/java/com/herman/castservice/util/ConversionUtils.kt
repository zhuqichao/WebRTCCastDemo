package com.herman.castservice.util

import com.herman.castservice.util.Md5Encoder.encodeBase64

object ConversionUtils {

    fun md5CodeEncode(longUrl: String, urlLength: Int): String {
        var length = urlLength
        if (length < 4) {
            length = 8 // defalut length
        }
        val sbBuilder = StringBuilder(length + 2)
        var md5Hex = ""
        var nLen = 0
        while (nLen < length) {
            // 这个方法是先 md5 再 base64编码 参见
            md5Hex = encodeBase64(md5Hex + longUrl)
            for (element in md5Hex) {
                val c = element
                if (c != '/' && c != '+') {
                    sbBuilder.append(c)
                    nLen++
                }
                if (nLen == length) {
                    break
                }
            }
        }
        return sbBuilder.toString().uppercase()
    }

}