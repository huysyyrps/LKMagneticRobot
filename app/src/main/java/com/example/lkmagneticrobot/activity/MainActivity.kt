package com.example.lkmagneticrobot.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.constant.BaseActivity
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.util.*
import com.example.lkmagneticrobot.util.Netty.BaseTcpClient
import com.example.lkmagneticrobot.util.Netty.BytesHexChange
import com.example.lkmagneticrobot.util.Netty.NettyTcpClient
import com.example.lkmagneticrobot.util.Netty.SendCallBack
import com.example.lkmagneticrobot.util.dialog.DialogUtil
import com.example.lkmagneticrobot.util.mediaprojection.MediaUtil
import com.example.lkmagneticrobot.util.thread.BaseThread
import com.littlegreens.netty.client.listener.NettyClientListener
import com.littlegreens.netty.client.status.ConnectState
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_right.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : BaseActivity(), View.OnClickListener, NettyClientListener<String> {
    var runing = true
    lateinit var bmp: Bitmap

//    private val yolov5ncnn = YoloV5Ncnn()
    private lateinit var mediaManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    val timer = Timer()
    var mNettyTcpClient: NettyTcpClient? = null
    var baseTcpClient: BaseTcpClient? = null
    var firstData = ""
    var secondData = ""
    var LEDState = Constant.LDGOPEN


    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //不息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//region
//        if (!yolov5ncnn.Init(assets)) {
//            "yolov5ncnn Init failed".showToast(this)
//            return
//        }
//endregion
        //是否通过全部权限
        DialogUtil().requestPermission(this, object : PermissionallBack {
            override fun permissionState(state: Boolean) {
                object : Thread() {
                    override fun run() {
                        while (runing) {
                            BaseThread.getVideoPhoto(imageView)
                        }
                    }
                }.start()
            }
        })
        fpvWidget.url = Constant.FPVURL
        fpvWidget.start()
        //使用硬解
        fpvWidget?.usingMediaCodec = true
//        //使用软解
//        fpvWidget?.usingMediaCodec = false

        //点击事件
        imageView.setOnClickListener(this)
        fpvWidget.setOnClickListener(this)
        btnCamer.setOnClickListener(this)
        btnStartVideo.setOnClickListener(this)
        btnStopVideo.setOnClickListener(this)
        btnFile.setOnClickListener(this)
        btnSettingController.setOnClickListener(this)
        btnSettingParam.setOnClickListener(this)
        etYoke.setOnClickListener(this)
        heightView.HeightView()
        heightView.height = 100

        //控制LED开关
        sbSearchlight.isChecked = false
        sbSearchlight.setOnCheckedChangeListener { _, isChecked: Boolean ->
            LEDState = if (isChecked){
                Constant.LDGOPEN
            }else{
                Constant.CLOSE
            }
            BaseThread.ManController(LEDState)
        }
//        //初始化连接
        connectSocket()

        //定时读取防止视频长时间卡顿
        timer.scheduleAtFixedRate(0, 1000*60) {
            fpvWidget.invalidate()
        }
    }

    //链接socket
    private fun connectSocket() {
        baseTcpClient = BaseTcpClient.getInstance()
        mNettyTcpClient = baseTcpClient?.initTcpClient(Constant.SERVERIP, Constant.SERVERPORT)
        mNettyTcpClient?.setListener(this) //设置TCP监听
        baseTcpClient?.tcpClientConntion(mNettyTcpClient)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.imageView -> {
                val linearParams = fpvWidget.layoutParams
                linearParams.height = 225
                linearParams.width = 400
                fpvWidget.bringToFront()
                fpvWidget.layoutParams = linearParams
                val linearParams1 = imageView.layoutParams
                linearParams1.height = 1200
                linearParams1.width = 1920 / 3 * 2 - 20
                imageView.layoutParams = linearParams1
            }
            R.id.fpvWidget -> {
                val linearParams = imageView.layoutParams
                linearParams.height = 225
                linearParams.width = 400
                imageView.bringToFront()
                imageView.layoutParams = linearParams

                val linearParams1 = fpvWidget.layoutParams
                linearParams1.height = 1200
                linearParams1.width = 1920 / 3 * 2 - 20
                fpvWidget.layoutParams = linearParams1
            }
            R.id.btnCamer -> {
                mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_ONE)
                } else {
                    mMediaProjection?.let {
                        MediaUtil.captureImages(this@MainActivity, it,"main")
                    }
                }
            }
            R.id.btnStartVideo -> {
                mediaManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                if (mMediaProjection == null) {
                    val captureIntent: Intent = mediaManager.createScreenCaptureIntent()
                    startActivityForResult(captureIntent, Constant.TAG_TWO)
                } else {
                    mMediaProjection?.let {
                        MediaUtil.startMedia(this@MainActivity, it,"main")
                        btnStartVideo.visibility = View.GONE
                        btnStopVideo.visibility = View.VISIBLE
                    }
                }
            }
            R.id.btnStopVideo -> {
                MediaUtil.stopMedia()
                btnStartVideo.visibility = View.VISIBLE
                btnStopVideo.visibility = View.GONE
            }
            R.id.btnFile -> {
                MainUi.showPopupMenu(btnFile, "Desc", this)
//                startActivity(Intent(this,UsbSerialActivity::class.java))
            }
            R.id.btnSettingController -> {
                sendControllerData()
            }
            R.id.btnSettingParam -> {
                val autoSpeedHex = ByteDataChange.singleToHex(etAutoSpeed.text.toString().toFloat()).toString()
                val autoDistanceHex = ByteDataChange.singleToHex(etAutoDistance.text.toString().toFloat()).toString()
                var magneticDistanceHex = ByteDataChange.addZeroForNum(Integer.toHexString(etMagneticDistance.text.toString().toInt()),2) .toString()
                var magneticTimeHex = ByteDataChange.addZeroForNum(Integer.toHexString(etMagneticTime.text.toString().toInt()),2) .toString()
                var sprayTimeHex = ByteDataChange.addZeroForNum(Integer.toHexString(etSprayTime.text.toString().toInt()),2) .toString()
                var sprayIntervalHex = ByteDataChange.addZeroForNum(Integer.toHexString(etSprayInterval.text.toString().toInt()),2) .toString()
                var data = "A10406$autoSpeedHex$autoDistanceHex$magneticDistanceHex$magneticTimeHex$sprayTimeHex$sprayIntervalHex"
                data = "$data${ByteDataChange.HexStringToBytes(data)}"
                val arrayData = BytesHexChange.HexStringToByteArr(data)
                sendData(arrayData)
            }
            R.id.etYoke -> {
                MainUi.showLightMenu(etYoke, this, object : MenuCallBack{
                    override fun menuCallBack(data: String) {
                        etYoke.setText(data)
                        sendControllerData()
                    }

                })
            }
        }
    }

    fun sendControllerData(){
        var protectBatteryHex = ByteDataChange.addZeroForNum(Integer.toHexString(etProtectBattery.text.toString().toInt()),2) .toString()
        val protectCurrentHex = ByteDataChange.singleToHex(etProtectCurrent.text.toString().toFloat()).toString()
        val limitationSpeedHex = ByteDataChange.singleToHex(etLimitationSpeed.text.toString().toFloat()).toString()
        var state = etYoke.text.toString()
        var lightState = "00"
        lightState = if(state==resources.getString(R.string.light_white)){
            "00"
        }else{
            "01"
        }
        var data = "A10304$protectBatteryHex$lightState$protectCurrentHex$limitationSpeedHex"
        data = "$data${ByteDataChange.HexStringToBytes(data)}"
        val arrayData = BytesHexChange.HexStringToByteArr(data)
        sendData(arrayData)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constant.TAG_ONE -> {
                    mMediaProjection = data?.let { mediaManager.getMediaProjection(resultCode, it) }
                    mMediaProjection?.let { MediaUtil.captureImages(this, it,"main") }
                }
                Constant.TAG_TWO -> {
                    mMediaProjection = data?.let { mediaManager.getMediaProjection(resultCode, it) }
                    mMediaProjection?.let { MediaUtil.startMedia(this, it,"main") }
                    btnStartVideo.visibility = View.GONE
                    btnStopVideo.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fpvWidget.stop()
        mNettyTcpClient?.disconnect()
    }

    //发送数据
    fun sendData(data: ByteArray) {
        baseTcpClient!!.sendTcpData(data, object : SendCallBack {
            override fun success(success: String?) {
                LogUtil.e("TAG","发送成功")
            }
            override fun faild(message: String?) {
                LogUtil.e("TAG","发送失败")
            }
        })
    }

    override fun onMessageResponseClient(backData: String, index: Int) {
        Log.e("TAG", backData!!)
//region
        var stringData = ""
        if (backData.startsWith("B1")&&backData.endsWith("0D0A")){
            stringData = backData
            firstData = ""
            analysisData(stringData)
            return
        }else if (backData.startsWith("B1") && !backData.endsWith("0D0A")){
//            if (backData.contains("0D0A")){
//                var token = StringTokenizer(backData, "0D0A");
//                stringData = token.nextToken()+"0D0A"
//                firstData = ""
//                var endData = backData.replace(stringData,"")
//                if (endData.startsWith("B1")){
//                    firstData = endData
//                }
//                return
//            }else{
//                firstData = backData
//                return
//            }
            Log.e("TAG","开始B1,结束不是$backData" )
            firstData = backData
            return
        }else if (backData.endsWith("0D0A") && !backData.startsWith("B1")){
            if (firstData.isNotEmpty()){
                stringData = firstData+backData
                firstData = ""
                Log.e("TAG","开始不是,结束是$backData" )
                analysisData(stringData)
                return
            }
        }else if (!backData.startsWith("B1") && !backData.endsWith("0D0A")){
            firstData+=backData
            return
        }
    }
    private fun analysisData(stringData:String){
        Log.e("TAG", stringData!!)
        if (stringData.startsWith("B101") && stringData.length == 14) {
            if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                val data = BytesHexChange.HexStringToByteArr("A10101A3")
                sendData(data)
            }
        }
        if (stringData.startsWith("B102") && stringData.length == 44) {
            if (ByteDataChange.HexStringToBytes(stringData.substring(0, 38)) == stringData.subSequence(38, 40)) {
                //主电源电量
                val battery = Integer.valueOf(stringData.substring(6, 8), 16)
                //当前工作电流
                val workCurrent = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(8, 16), 16))
                //行进距离
                val distance = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(16, 24), 16))
                //行进速度
                val speed = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(24, 32), 16))
                //当前抬升位置
                val height = Integer.valueOf(stringData.substring(32, 34), 16)
                //运行模式
                val runType = Integer.valueOf(stringData.substring(34, 36), 16)
                //遥控器通讯帧率
                val frequency = Integer.valueOf(stringData.substring(36, 38), 16)
                CoroutineScope(Dispatchers.Main).launch {
                    batteryView.BatteryView()
                    batteryView.setProgress(battery,0)
                    etWorkCurrent.setText("$workCurrent")
                    etDistance.setText("$distance")
                    etSpeed.setText("$speed")
                    etFrequency.setText("$frequency")
                    heightView.HeightView()
                    heightView.height = height
                    tvHeight.text = "抬升高度$height"
                    when (runType) {
                        0 -> {
                            etRunType.setText(resources.getString(R.string.hand_type))
                        }
                        1 -> {
                            etRunType.setText(resources.getString(R.string.semi_auto))
                        }
                        2 -> {
                            etRunType.setText(resources.getString(R.string.auto))
                        }
                    }
                }
                // region
                // 定时读取
//                                    timer.scheduleAtFixedRate(0, 1000) {
//                                        val arrayData = toBytes("A10206A8")
//                                        mServiceConnection?.sendData(arrayData)
//                                    }
                //endregion
                val data = BytesHexChange.HexStringToByteArr("A10206A9")
                sendData(data)
            }
        }
        if (stringData.startsWith("B103") && stringData.length == 34) {
            if (ByteDataChange.HexStringToBytes(stringData.substring(0, 28)) == stringData.subSequence(28, 30)) {
                //保护电量
                val protectBattery: Int = Integer.valueOf(stringData.substring(6, 8), 16)
                //磁轭补光灯类型
                val changeElectQuantity = Integer.valueOf(stringData.substring(8, 10), 16)
                //保护电流
                val protectCurrent = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(10, 18), 16))
                //上限制速度
                val limitation_speed = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(18, 26), 16))
                CoroutineScope(Dispatchers.Main).launch {
                    etProtectBattery.setText("$protectBattery")
                    etProtectCurrent.setText("$protectCurrent")
                    etLimitationSpeed.setText("$limitation_speed")
                    if (changeElectQuantity==0){
                        etYoke.text = resources.getText(R.string.light_white)
                    }else if (changeElectQuantity==1){
                        etYoke.text = resources.getText(R.string.light_black)
                    }
                }
            }
        }
        if (stringData.startsWith("B104") && stringData.length == 38) {
            if (ByteDataChange.HexStringToBytes(stringData.substring(0, 32)) == stringData.subSequence(32, 34)) {
                //自动运行速度
                val autoSpeed = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(6, 14), 16))
                //自动运行距离
                val autoDistance = java.lang.Float.intBitsToFloat(Integer.valueOf(stringData.substring(14, 22), 16))
                //间隔磁化距离
                val magneticDistance = Integer.valueOf(stringData.substring(22, 24), 16)
                //单点磁化时间
                val magneticTime = Integer.valueOf(stringData.substring(24, 26), 16)
                //喷涂时间
                val sprayTime = Integer.valueOf(stringData.substring(26, 28), 16)
                //喷涂时间
                val sprayInterval = Integer.valueOf(stringData.substring(28, 30), 16)
                CoroutineScope(Dispatchers.Main).launch {
                    etAutoSpeed.setText("$autoSpeed")
                    etAutoDistance.setText("$autoDistance")
                    etMagneticDistance.setText("$magneticDistance")
                    etMagneticTime.setText("$magneticTime")
                    etSprayTime.setText("$sprayTime")
                    etSprayInterval.setText("$sprayInterval")
                }
            }
        }
    }



    override fun onClientStatusConnectChanged(statusCode: Int, index: Int) {
        //连接状态回调
        when (statusCode) {
            ConnectState.STATUS_CONNECT_SUCCESS -> {
                Log.e("TAG", "成功")
            }
            ConnectState.STATUS_CONNECT_CLOSED -> {
                Log.e("TAG", "断开")
            }
            ConnectState.STATUS_CONNECT_ERROR -> {
                Log.e("TAG", "失败")
            }
        }
    }

}