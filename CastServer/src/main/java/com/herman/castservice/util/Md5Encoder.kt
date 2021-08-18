package com.herman.castservice.util

import org.apache.tomcat.util.codec.binary.Base64
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Md5Encoder {

    private fun rawEncode(data: ByteArray): ByteArray {
        val digest: MessageDigest
        return try {
            digest = MessageDigest.getInstance("MD5")
            digest.update(data, 0, data.size)
            digest.digest()
        } catch (e: NoSuchAlgorithmException) {
            byteArrayOf()
        }
    }

    /**
     * 将md5 编码进行base64编码，去掉最后的两个==，16位的md5码base64后最后两位肯定是==
     *
     * @param data    需要编码的 数据
     * @param urlSafe 返回url合法字符
     * @return 将md5 编码进行base64编码，去掉最后的两个==
     */
    @JvmOverloads
    fun encodeBase64(data: ByteArray, urlSafe: Boolean = false): String {
        val md5Code = rawEncode(data)
        return String(if (urlSafe) Base64.encodeBase64URLSafe(md5Code) else Base64.encodeBase64(md5Code), 0, 22)
    }

    @JvmOverloads
    fun encodeBase64(data: String, urlSafe: Boolean = false): String {
        return try {
            encodeBase64(data.toByteArray(StandardCharsets.UTF_8), urlSafe)
        } catch (e: UnsupportedEncodingException) {
            ""
        }
    }
}