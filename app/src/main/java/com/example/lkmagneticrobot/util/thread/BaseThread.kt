package com.example.lkmagneticrobot.util.thread

import android.graphics.BitmapFactory
import android.os.SystemClock
import android.util.Log
import android.widget.ImageView
import com.example.lkmagneticrobot.constant.Constant
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL

object BaseThread {
    private var pw: PrintWriter? = null
    private var socket: Socket? = null

    //磁探机视频
    fun getVideoPhoto(imageView: ImageView) {
        try {
            var inputstream: InputStream? = null
            //创建一个URL对象
            val videoUrl = URL("${Constant.URL}?action=snapshot")
            //利用HttpURLConnection对象从网络中获取网页数据
            val conn = videoUrl.openConnection() as HttpURLConnection
            //设置输入流
            conn.doInput = true
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            //连接
            conn.connect()
            //得到网络返回的输入流
            inputstream = conn.inputStream
            //创建出一个bitmap
            val bmp = BitmapFactory.decodeStream(inputstream)
            //region
//            var objects: Array<YoloV5Ncnn.Obj>
//            CoroutineScope(Dispatchers.Main).launch {
//                objects = yolov5ncnn.Detect(bmp, false)
//                if (objects == null || objects.isEmpty()) {
//                    imageView.setImageBitmap(bmp)
//                } else {
//                    val rgba = bmp.copy(Bitmap.Config.ARGB_8888, true)
//                    val canvas = Canvas(rgba)
//                    for (i in objects!!.indices) {
//                        canvas.drawRect(
//                            objects[i].x,
//                            objects[i].y, objects[i].x + objects[i].w, objects[i].y + objects[i].h, BasePaint.getLinePaint()
//                        )
//                        val text = objects[i].label + " = " + String.format("%.1f", objects[i].prob * 100) + "%"
//                        val text_width: Float = BasePaint.getTextpaint().measureText(text) + 10
//                        val text_height: Float = -BasePaint.getTextpaint().ascent() + BasePaint.getTextpaint().descent() + 10
//                        var x = objects[i].x
//                        var y = objects[i].y - text_height
//                        if (y < 0) y = 0f
//                        if (x + text_width > rgba.width) x = rgba.width - text_width
//                        canvas.drawText(
//                            text, x, y - BasePaint.getTextpaint().ascent(),
//                            BasePaint.getTextpaint()
//                        )
//                    }
//                    imageView.setImageBitmap(rgba)
//                }
//            }
            //endregion
            imageView.setImageBitmap(bmp)
            //关闭HttpURLConnection连接
            conn.disconnect()
        } catch (ex: Exception) {
            Log.e("XXX", ex.toString())
        } finally {
        }
    }

    //LED控制
    fun ManController(LEDState: String) {
        Thread { connectServer(LEDState) }.start()
    }
    private fun connectServer(LEDState: String) {
        var socket: Socket? = null
        while (socket == null) {
            try {
                socket = Socket(Constant.CONTROLLERIP, Constant.CONTROLLERPORT)
                this.socket = socket
                pw = PrintWriter(BufferedWriter(OutputStreamWriter(socket.getOutputStream())), true)
                pw?.write(LEDState)
                pw?.flush()
                socket.shutdownInput();
                socket.close();
            } catch (e: IOException) {
                SystemClock.sleep(1000)
            }
        }
    }
}