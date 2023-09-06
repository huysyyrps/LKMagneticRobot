package com.example.lkmagneticrobot.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.example.lkmagneticrobot.databinding.ActivityMainBinding
import com.example.lkmagneticrobot.constant.BaseBindingActivity
import com.example.lkmagneticrobot.util.Constant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

class MainActivity : BaseBindingActivity<ActivityMainBinding>() {
    var runing = true
    lateinit var bmp:Bitmap
    private var exitTime: Long = 0
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.action === KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - exitTime > 2000) {
                Toast.makeText(applicationContext, "再按一次退出程序", Toast.LENGTH_SHORT).show()
                exitTime = System.currentTimeMillis()
            } else {
                finish()
                exitProcess(0)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // object 对象表达式,创建一个匿名类，并重写 run() 方法
        object : Thread() {
            override fun run() {
                while (runing) {
                    getVideoPhoto()
                }
            }
        }.start()
    }

    private fun getVideoPhoto() {
        try {
            var inputstream: InputStream? = null
            //创建一个URL对象
            var videoUrl = URL("${Constant.URL}?action=snapshot")
            //利用HttpURLConnection对象从网络中获取网页数据
            var conn = videoUrl.openConnection() as HttpURLConnection
            //设置输入流
            conn.doInput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            //连接
            conn.connect()
            //得到网络返回的输入流
            inputstream = conn.inputStream
            //创建出一个bitmap
            bmp = BitmapFactory.decodeStream(inputstream)
            binding.imageView.setImageBitmap(bmp)
            //关闭HttpURLConnection连接
            conn.disconnect()
        } catch (ex: Exception) {
            Log.e("XXX", ex.toString())
        } finally {
        }
    }
}