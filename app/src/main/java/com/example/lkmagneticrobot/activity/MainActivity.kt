package com.example.lkmagneticrobot.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.CompoundButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.constant.BaseActivity
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.util.*
import com.example.lkmagneticrobot.util.BinaryChange.toBytes
import com.example.lkmagneticrobot.util.Netty.BaseTcpClient
import com.example.lkmagneticrobot.util.Netty.BytesHexChange
import com.example.lkmagneticrobot.util.Netty.NettyTcpClient
import com.example.lkmagneticrobot.util.Netty.SendCallBack
import com.example.lkmagneticrobot.util.dialog.DialogUtil
import com.example.lkmagneticrobot.util.mediaprojection.MediaUtil
import com.littlegreens.netty.client.listener.NettyClientListener
import com.littlegreens.netty.client.status.ConnectState
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_right.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.*
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate
import kotlin.system.exitProcess


//, View.OnClickListener
class MainActivity : BaseActivity(), View.OnClickListener, NettyClientListener<String> {
    var runing = true
    lateinit var bmp: Bitmap
    private var exitTime: Long = 0
//    private val yolov5ncnn = YoloV5Ncnn()
    private lateinit var mediaManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    //FPV控制
//    private lateinit var mSerialPortControl: SerialPortControl
    val timer = Timer()
    //超时时间
    private val CONNECTION_TIMEOUT = 20 * 1000
    //输出流
    private var mavOut : BufferedOutputStream? = null
    //输入流
    public var mavIn : BufferedInputStream? = null
    //读取线程
    private var mReadThread : ReadThread? = null

    var mNettyTcpClient: NettyTcpClient? = null
    var baseTcpClient: BaseTcpClient? = null
    var firstData = ""
    //usb连接实例
    //private var mSerialPortConnection: SerialPortConnection? = null
    private var pw: PrintWriter? = null
    private var socket: Socket? = null
    var LEDState = Constant.LDGOPEN

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

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //不息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
//        if (!yolov5ncnn.Init(assets)) {
//            "yolov5ncnn Init failed".showToast(this)
//            return
//        }
        //是否通过全部权限
        DialogUtil().requestPermission(this, object : PermissionallBack {
            override fun permissionState(state: Boolean) {
                object : Thread() {
                    override fun run() {
                        while (runing) {
                            getVideoPhoto()
                        }
                    }
                }.start()
            }
        })
        //设置播放地址
        fpvWidget.url = "rtsp://192.168.144.108:554/stream=0"
        //开始播放
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

        sbSearchlight.isChecked = false
        sbSearchlight.setOnCheckedChangeListener { _, isChecked: Boolean ->
            LEDState = if (isChecked){
                Constant.LDGOPEN
            }else{
                Constant.CLOSE
            }
            Thread { connectServer() }.start()
        }
//        //初始化连接
//        initConnect()
        connectSocket()

        //定时读取
        timer.scheduleAtFixedRate(0, 1000*60) {
            fpvWidget.invalidate()
        }
    }

    private fun connectServer() {
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


    //链接socket
    fun connectSocket() {
        baseTcpClient = BaseTcpClient.getInstance()
        mNettyTcpClient = baseTcpClient?.initTcpClient(Constant.SERVERIP, Constant.SERVERPORT)
        mNettyTcpClient?.setListener(this) //设置TCP监听
        baseTcpClient?.tcpClientConntion(mNettyTcpClient)
    }

    private fun initConnect() {
        Thread{
            try {
                socket =  Socket()
                socket?.connect( InetSocketAddress("serverAddr", 1), CONNECTION_TIMEOUT)
                Thread.sleep(1000)
                mavOut =  BufferedOutputStream((socket?.getOutputStream()))
                mavIn =  BufferedInputStream(socket?.getInputStream())
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mReadThread = ReadThread()
            mReadThread?.start()
        }.start()
        /**
         *  //硬件串口实例
        mServiceConnection = SerialPortConnection.newBuilder("/dev/ttyMSM1", 115200)
        .flags(1 shl 13)
        .build()
        mServiceConnection?.setDelegate(object : SerialPortConnection.Delegate {
        @SuppressLint("SetTextI18n")
        override fun received(bytes: ByteArray, size: Int) {
        val stringData = ByteDataChange.ByteToString(bytes)
        Log.e("TAG", stringData)
        //在设备上电后1S周期向遥控器接收端发送包含遥控器通讯帧率的数据包
        if (stringData.startsWith("B101") && stringData.length == 10) {
        if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
        val arrayData = toBytes("A10101A3")
        mServiceConnection?.sendData(arrayData)
        }
        }
        if (stringData.startsWith("B103") && stringData.length == 30) {
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
        sbYoke.isSelected = true
        }else if (changeElectQuantity==1){
        sbYoke.isSelected = false
        }
        }
        //定时读取
        timer.scheduleAtFixedRate(0, 1000) {
        val arrayData = toBytes("A10206A8")
        mServiceConnection?.sendData(arrayData)
        }
        }
        }
        if (stringData.startsWith("B104") && stringData.length == 32) {
        if (ByteDataChange.HexStringToBytes(stringData.substring(0, 30)) == stringData.subSequence(30, 32)) {
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
        if (stringData.startsWith("B102") && stringData.length == 40) {
        if (ByteDataChange.HexStringToBytes(stringData.substring(0, 28)) == stringData.subSequence(28, 30)) {
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
        //定时读取
        timer.scheduleAtFixedRate(0, 1000) {
        val arrayData = toBytes("A10206A8")
        mServiceConnection?.sendData(arrayData)
        }
        }
        }
        }

        override fun connect() {
        LogUtil.e("TAG", "数传连接成功")
        timer.scheduleAtFixedRate(0, 1000) {
        val arrayData = toBytes("A1")
        mServiceConnection?.sendData(
        arrayData
        )
        }

        }
        })
        try {
        //打开串口
        mServiceConnection?.openConnection()
        //            LogUtil.e("TAG", "连接成功")
        //            mServiceConnection.sendData("".toByteArray())
        } catch (e: java.lang.Exception) {
        e.printStackTrace()
        }
        mSerialPortControl = SerialPortControl(mServiceConnection)
         */
    }

    /**
     * 数据读取线程
     */
    inner class ReadThread:Thread(){
        override fun run() {
            try {
                var buf = ByteArray(256)
                var size = 0
                while (!isInterrupted ){
                    if(mavIn != null){
                        sleep(1000)
                        var len = mavIn!!.read(buf)
                        if(len > 0){
                            var tempArray = ByteArray(len)
                            System.arraycopy(buf,0,tempArray,0,len)
                            var stringData = String2ByteArrayUtils.bytes2Hex(tempArray)
                            if (stringData != null) {
                                LogUtil.e("TAG",stringData)
                            }
                            if (stringData.startsWith("B101") && stringData.length == 10) {
                                if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                                    sendData("A10101A3")
                                }
                            }
                            if (stringData.startsWith("B102") && stringData.length == 40) {
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
                                }
                            }
                            if (stringData.startsWith("B103") && stringData.length == 30) {
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
                                            sbYoke.isSelected = true
                                        }else if (changeElectQuantity==1){
                                            sbYoke.isSelected = false
                                        }
                                    }
//                                    //定时读取
//                                    timer.scheduleAtFixedRate(0, 1000) {
//                                        val arrayData = toBytes("A10206A8")
//                                        mServiceConnection?.sendData(arrayData)
//                                    }
                                }
                            }
                            if (stringData.startsWith("B104") && stringData.length == 34) {
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
                        }
                    }
            }catch (e:InterruptedException){
                e.printStackTrace()
            }catch (e: IOException){
                e.printStackTrace()
            }

        }
    }

    /**
     * 发送数据
     */
    private fun sendData(data: String) {
        Thread{
//            var arrayData = "A10101A3".toByteArray()
            val arrayData = toBytes(data)
            mavOut?.write(arrayData)
            mavOut?.flush()
        }.start()
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
//                startActivity(Intent(this,UsbSerialActivity::class.java))
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


    private fun getVideoPhoto() {
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
            bmp = BitmapFactory.decodeStream(inputstream)
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

            imageView.setImageBitmap(bmp)
            //关闭HttpURLConnection连接
            conn.disconnect()
        } catch (ex: Exception) {
            Log.e("XXX", ex.toString())
        } finally {
        }
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
//        if (mServiceConnection != null) {
//            try {
//                mServiceConnection!!.closeConnection()
//            } catch (e: IOException) {
//                e.printStackTrace()
//            }
//        }
//        mServiceConnection = null
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
        //region
//        //6为帧头、命令码、检验的长度和
//        Log.e("TAG", backData!!)
//        var stringData = ""
//        if (backData.startsWith("B10")&&backData.endsWith("XXX")){
//            stringData = backData
//        }else if (backData.startsWith("B10") && !backData.endsWith("XXX")){
//            if (backData.contains("XXX")){
//                var token = StringTokenizer(backData, "XXX");
//                stringData = token.nextToken()+"XXX"
//                var endData = backData.replace(stringData,"")
//                if (endData.startsWith("B10")){
//                    firstData = endData
//                }
//            }else{
//                firstData = backData
//                return
//            }
//        }else if (!backData.startsWith("B10") && backData.endsWith("XXX")){
//            if (firstData.isNullOrEmpty()){
//                stringData = firstData+backData
//            }
//        }else if (!backData.startsWith("B10") && !backData.endsWith("XXX")){
//            firstData+=backData
//            return
//        }
        //endregion
        var stringData = ""
        if (backData.startsWith("B10")){
            firstData = backData
            return
        }else{
            stringData = firstData+backData
        }
        Log.e("TAG", stringData!!)
        if (stringData.startsWith("B101") && stringData.length == 10) {
            if (ByteDataChange.HexStringToBytes(stringData.substring(0, 8)) == stringData.subSequence(8, 10)) {
                sendData("A10101A3")
            }
        }
        if (stringData.startsWith("B102") && stringData.length == 40) {
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
            }
        }
        if (stringData.startsWith("B103") && stringData.length == 30) {
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
                        sbYoke.isSelected = true
                    }else if (changeElectQuantity==1){
                        sbYoke.isSelected = false
                    }
                }
                //region
//                                    //定时读取
//                                    timer.scheduleAtFixedRate(0, 1000) {
//                                        val arrayData = toBytes("A10206A8")
//                                        mServiceConnection?.sendData(arrayData)
//                                    }
                //endregion
            }
        }
        if (stringData.startsWith("B104") && stringData.length == 34) {
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
                val data = BytesHexChange.HexStringToByteArr("A10101A3")
                sendData(data)
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