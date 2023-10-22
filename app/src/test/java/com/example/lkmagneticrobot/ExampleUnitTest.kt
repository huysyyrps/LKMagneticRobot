package com.example.lkmagneticrobot

import com.example.lkmagneticrobot.util.LogUtil
import org.junit.Test

import org.junit.Assert.*
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        var str= "6874311加6843184351"
        var split ="加"
        var token = StringTokenizer(str, split);
        str = token.nextToken();
       LogUtil.e("TAG",str)
    }
}