package com.example.lkmagneticrobot.activity

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.YoloV5Ncnn
import com.example.lkmagneticrobot.constant.BaseBindingActivity
import com.example.lkmagneticrobot.databinding.ActivityMainBinding
import com.example.lkmagneticrobot.util.BasePaint
import com.example.lkmagneticrobot.util.Constant
import com.example.lkmagneticrobot.util.LogUtil
import com.example.lkmagneticrobot.util.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

//, View.OnClickListener
class MainActivity : BaseBindingActivity<ActivityMainBinding>(), View.OnClickListener{
    var runing = true
    lateinit var bmp:Bitmap
    private var exitTime: Long = 0
    private var width: Int = 0
    private var height: Int = 0
    private val yolov5ncnn = YoloV5Ncnn()

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
        //不息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!yolov5ncnn.Init(assets)) {
            "yolov5ncnn Init failed".showToast(this)
            return
        }
        object : Thread() {
            override fun run() {
                while (runing) {
                    getVideoPhoto()
                }
            }
        }.start()

        //设置播放地址
        binding.fpvWidget.url = "rtsp://192.168.144.108:554/stream=0"
        //开始播放
        binding.fpvWidget.start()
        //点击事件
        binding.imageView.setOnClickListener (this)
        binding.fpvWidget.setOnClickListener(this)

        val wm = this.windowManager
        width = wm.defaultDisplay.width
        height = wm.defaultDisplay.height
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.imageView->{
                val linearParams =  binding.fpvWidget.layoutParams
                linearParams.height = 200
                linearParams.width = 300
                binding.fpvWidget.bringToFront()
                binding.fpvWidget.layoutParams = linearParams
                val linearParams1 =  binding.imageView.layoutParams
                linearParams1.height = height
                linearParams1.width = width/3*2-20
                binding.imageView.layoutParams = linearParams1
            }
            R.id.fpvWidget->{
                val linearParams =  binding.imageView.layoutParams
                linearParams.height = 200
                linearParams.width = 300
                binding.imageView.bringToFront()
                binding.imageView.layoutParams = linearParams

                val linearParams1 =  binding.fpvWidget.layoutParams
                linearParams1.height = height
                linearParams1.width = width/3*2-20
                binding.fpvWidget.layoutParams = linearParams1
            }
        }
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
            var objects: Array<YoloV5Ncnn.Obj>
            /**
             * CoroutineScope(Dispatchers.Main).launch {
            objects = yolov5ncnn.Detect(bmp, false)
            if (objects == null || objects.isEmpty()) {
            binding.imageView.setImageBitmap(bmp)
            }else{
            val rgba = bmp.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(rgba)
            for (i in objects!!.indices) {
            canvas.drawRect(objects[i].x,
            objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, BasePaint.getLinePaint())
            val text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%"
            val text_width: Float = BasePaint.getTextpaint().measureText(text) + 10
            val text_height: Float = -BasePaint.getTextpaint().ascent() + BasePaint.getTextpaint().descent() + 10
            var x = objects[i].x
            var y = objects[i].y - text_height
            if (y < 0) y = 0f
            if (x + text_width > rgba.width) x = rgba.width - text_width
            canvas.drawText(text, x, y - BasePaint.getTextpaint().ascent(),
            BasePaint.getTextpaint()
            )
            }
            binding.imageView.setImageBitmap(rgba)
            }
            }
             */
            binding.imageView.setImageBitmap(bmp)
            //关闭HttpURLConnection连接
            conn.disconnect()
        } catch (ex: Exception) {
            Log.e("XXX", ex.toString())
        } finally {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.fpvWidget.stop()
    }

}