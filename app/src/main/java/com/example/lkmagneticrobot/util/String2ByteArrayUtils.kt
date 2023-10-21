package com.example.lkmagneticrobot.util

import android.text.TextUtils
import kotlin.experimental.and

/**
 * author:CQD

 * e-mail:634897993@qq.com

 * data:  2020/4/29

 * desc:
 *
 * version:
 */
object String2ByteArrayUtils {

    private val HEXES = charArrayOf(
        '0', '1', '2', '3',
        '4', '5', '6', '7',
        '8', '9', 'A', 'B',
        'C', 'D', 'E', 'F'
    )

    fun hexString2ByteArray(arg: String): ByteArray? {
        if (!TextUtils.isEmpty(arg)) {
            /* 1.先去除String中的' '，然后将String转换为char数组 */
            val newArray = CharArray(512)
            val array = arg.toCharArray()
            var length = 0
            for (i in array.indices) {
                if (array[i] != ' ') {
                    newArray[length] = array[i]
                    length++
                }
            }
            /* 将char数组中的值转成一个实际的十进制数组 */
            val evenLength = if (length % 2 == 0) length else length + 1
            if (evenLength != 0) {
                val data = IntArray(evenLength)
                data[evenLength - 1] = 0
                for (i in 0 until length) {
                    when {
                        newArray[i] in '0'..'9' -> {
                            data[i] = newArray[i] - '0'
                        }
                        newArray[i] in 'a'..'f' -> {
                            data[i] = newArray[i] - 'a' + 10
                        }
                        newArray[i] in 'A'..'F' -> {
                            data[i] = newArray[i] - 'A' + 10
                        }
                    }
                }
                /* 将 每个char的值每两个组成一个16进制数据 */
                val byteArray = ByteArray(evenLength / 2)
                for (i in 0 until evenLength / 2) {
                    byteArray[i] = (data[i * 2] * 16 + data[i * 2 + 1]).toByte()
                }
                return byteArray
            }
        }
        return byteArrayOf()
    }



    /**
     * byte数组 转换成 16进制大写字符串
     */
    fun bytes2Hex(bytes: ByteArray?): String? {

        if (bytes == null || bytes.isEmpty()) {
            return null
        }

        val hex = StringBuilder()
        for (b in bytes) {
            hex.append(HEXES[(b.toInt() shr 4) and 0x0F])
            hex.append(HEXES[  ((b and 0x0F).toInt())])
        }
        return hex.toString()
    }

    fun byte2Hex(b: Byte): String? {
        val hex = StringBuilder()
        hex.append(HEXES[(b.toInt() shr 4) and 0x0F])
        hex.append(HEXES[  ((b and 0x0F).toInt())])
        return hex.toString()
    }

    /**
     * 16进制字符串 转换为对应的 byte数组
     */
    fun hex2Bytes(hex: String?): ByteArray? {
        var hex = hex
        if (hex == null || hex.isEmpty()) {
            return null
        }
        hex = hex.replace(" ", "")
        val hexChars = hex.toCharArray()
        val bytes =
            ByteArray(hexChars.size / 2) // 如果 hex 中的字符不是偶数个, 则忽略最后一个
        for (i in bytes.indices) {
            bytes[i] =
                ("" + hexChars[i * 2] + hexChars[i * 2 + 1]).toInt(16).toByte()
        }
        return bytes
    }

    /**
     * byte转int类型
     * 如果byte是负数，则转出的int型是正数
     * @param b
     * @return
     */
    fun  byteToInt(b:Byte):Int{
        return (b.toInt() and 0xff)
    }

    fun bytes2int(bytes: ByteArray): Int {
        var value = 0
        for (i in 0..3) {
            val shift = (4 - 1 - i) * 8
            value += bytes[i].toInt() and 0x000000FF shl shift // 往高位游
        }
        return value
    }

}