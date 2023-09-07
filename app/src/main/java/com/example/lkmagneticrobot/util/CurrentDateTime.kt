package com.example.lkmagneticrobot.util

import java.text.SimpleDateFormat
import java.util.*

object CurrentDateTime {
    /**
     * 获取当前时间
     */
    fun getNowDate(): String? {
        val format = SimpleDateFormat("yyyy-MM-dd HH.mm.ss", Locale.US)
        return format.format(Date())
    }
}