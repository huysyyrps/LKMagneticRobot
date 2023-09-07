package com.example.lkmagneticrobot.util.mediaprojection

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.util.CurrentDateTime
import java.io.File
import java.io.IOException

object MediaRecorderUtil {
    fun getMediaRecorder(context: Context?, path: String): MediaRecorder? {
        val mediaRecorder = MediaRecorder()
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC) //音频载体
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE) //视频载体
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) //输出格式
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT) //音频格式
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264) //视频格式
        mediaRecorder.setVideoSize(2400, 1500) //size
        mediaRecorder.setVideoFrameRate(30) //帧率
        //创建文件夹
        val dir = File(context?.externalCacheDir.toString()+ "/" + Constant.SAVE_VIDEO_PATH + "/")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        //创建文件名
        val fileName: String = CurrentDateTime.getNowDate() + ".mp4"

        //设置文件位置
        val filePath = "$dir/$fileName"
        mediaRecorder.setOutputFile(filePath)
        mediaRecorder.setVideoEncodingBitRate(3 * 1920 * 1080) //比特率
        mediaRecorder.setOrientationHint(0) //旋转角度
        try {
            mediaRecorder.prepare()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            Log.e("XXX", e.toString())
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("XXX", e.toString())
        }
        return mediaRecorder
    }
}