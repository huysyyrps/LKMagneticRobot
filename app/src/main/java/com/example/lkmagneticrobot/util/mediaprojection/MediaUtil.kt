package com.example.lkmagneticrobot.util.mediaprojection

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import com.example.lkmagneticrobot.MyApplication
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.activity.MainActivity
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.util.BitmapSave
import com.example.lkmagneticrobot.util.showToast
import java.nio.ByteBuffer

object MediaUtil {
    private var imageReader: ImageReader? = null
    private var mediaRecorder: MediaRecorder? = null
    //创建一个虚屏VirtualDisplay，内含一个真实的Display对象
    private var mVirtualDisplay: VirtualDisplay? = null
    private var isGot: Boolean = false
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant", "Range")
    @JvmStatic
    open fun captureImages(activity: Activity, mMediaProjection:MediaProjection, tag:String){
        Handler(Looper.myLooper()!!).postDelayed({
            //配置ImageReader
            val dm = activity.resources.displayMetrics
            var height = 0
            if (tag=="main"){
                height = dm.heightPixels-60
            }else if (tag=="usb"){
                height = dm.heightPixels
            }
            imageReader = ImageReader.newInstance(
                dm.widthPixels, height,
                PixelFormat.RGBA_8888, 1
            ).apply {
                setOnImageAvailableListener({
                    //这里页面帧发生变化时就会回调一次，我们只需要获取一张图片，加个标记位，避免重复
                    //                        if (!isGot) {
                    //                            isGot = true
                    //                            //这里就可以保存图片了
                    //                            savePicTask(it,activity,mMediaProjection)
                    //                        }
                    savePicTask(it,activity,mMediaProjection)
                }, null)

                //把内容投射到ImageReader 的surface
                mMediaProjection.createVirtualDisplay(
                    "image", dm.widthPixels, dm.heightPixels, dm.densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, surface, null, null
                )
            }
        }, 400)
    }

    /**
     * 保存图片
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun savePicTask(reader: ImageReader, activity: Activity, mMediaProjection: MediaProjection) {
        var image: Image? = null
        try {
            //获取捕获的照片数据
            image = reader.acquireLatestImage()
            val width = image.width
            val height = image.height
            //拿到所有的 Plane 数组
            val planes = image.planes
            val plane = planes[0]

            val buffer: ByteBuffer = plane.buffer
            //相邻像素样本之间的距离，因为RGBA，所以间距是4个字节
            val pixelStride = plane.pixelStride
            //每行的宽度
            val rowStride = plane.rowStride
            //因为内存对齐问题，每个buffer 宽度不同，所以通过pixelStride * width 得到大概的宽度，
            //然后通过 rowStride 去减，得到大概的内存偏移量，不过一般都是对齐的。
            val rowPadding = rowStride - pixelStride * width
            // 创建具体的bitmap大小，由于rowPadding是RGBA 4个通道的，所以也要除以pixelStride，得到实际的宽
            val bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            var saveTag = BitmapSave.saveBitmap(Constant.SAVE_IMAGE_PATH,activity,bitmap)
            if (saveTag)
                MyApplication.context.resources.getString(R.string.save_success).showToast(activity)
            else
                MyApplication.context.resources.getString(R.string.save_fail).showToast(activity)

//            mMediaProjection?.stop()
            imageReader?.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            //记得关闭 image
            try {
                image?.close()
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 录屏
     */
    @JvmStatic
    fun startMedia(activity: Activity, mediaProjection: MediaProjection, tag:String) {
        //获取mediaRecorder
        mediaRecorder = MediaRecorderUtil.getMediaRecorder(activity, Constant.SAVE_VIDEO_PATH)
        var height = 0
        if (tag=="main"){
            height=1650
        }else if (tag=="usb"){
            height=1500
        }
        mVirtualDisplay = mediaProjection!!.createVirtualDisplay(
            "你的name",
            2400, height, 1,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder!!.surface,
            null, null
        )
        //开始录制
        //开始录制
        try {
            mediaRecorder!!.start()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    /**
     * 结束录屏
     */
    @JvmStatic
    fun stopMedia() {
        if (mediaRecorder != null) {
            mediaRecorder?.stop()
            mediaRecorder==null
        }
    }
}