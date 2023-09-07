package com.example.lkmagneticrobot.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object BitmapSave {
    /**
     * 保存图片方法
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun saveBitmap(
        local: String,
        context: Context,
        bitmap: Bitmap
    ) :Boolean{
        try {
            val dir = context.externalCacheDir.toString()+ "/" + local + "/" //图片保存的文件夹名
            val file = File(dir)
            //如果不存在  就mkdirs()创建此文件夹
            if (!file.exists()) {
                file.mkdirs()
            }
            //将要保存的图片文件
            val mFile = File(dir + CurrentDateTime.getNowDate()+".png")
            val outputStream = FileOutputStream(mFile) //构建输出流
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream) //compress到输出outputStream
            val uri = Uri.fromFile(mFile) //获得图片的uri
            context.sendBroadcast(
                Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    uri
                )
            ) //发送广播通知更新图库，这样系统图库可以找到这张图片
            outputStream.flush()
            outputStream.close()
            return true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }
}