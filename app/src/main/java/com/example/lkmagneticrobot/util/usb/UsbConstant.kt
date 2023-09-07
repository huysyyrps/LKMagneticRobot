package com.example.lkmagneticrobot.util.usb

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.os.Handler
import android.os.Looper
import com.example.lkmagneticrobot.R
import com.skydroid.android.usbserial.DeviceFilter
import com.skydroid.android.usbserial.USBMonitor
import com.skydroid.android.usbserial.USBMonitor.OnDeviceConnectListener
import com.skydroid.android.usbserial.USBMonitor.UsbControlBlock
import com.skydroid.fpvlibrary.usbserial.UsbSerialConnection
import com.skydroid.fpvlibrary.usbserial.UsbSerialControl
import com.skydroid.fpvlibrary.utils.BusinessUtils
import com.skydroid.fpvlibrary.video.FPVVideoClient
import com.skydroid.fpvlibrary.widget.GLHttpVideoSurface

@SuppressLint("StaticFieldLeak")
object UsbConstant {
    private lateinit var mContext: Activity
    //Usb监视器
    private var mUSBMonitor: USBMonitor? = null
    //Usb设备
    private var mUsbDevice: UsbDevice? = null
    private var mPreviewDualVideoView: GLHttpVideoSurface? = null
    //视频渲染
    private var mFPVVideoClient: FPVVideoClient? = null
    //usb连接实例
    private lateinit var mUsbSerialConnection: UsbSerialConnection
    //摄像头控制
    private var mUsbSerialControl: UsbSerialControl? = null
    private var mainHanlder = Handler(Looper.getMainLooper())

    fun setContext(context: Activity){
        mContext = context
    }

    fun init() {
        //初始化usb连接
        mUsbSerialConnection = UsbSerialConnection(mContext)
        mUsbSerialConnection.setDelegate(object : UsbSerialConnection.Delegate {
            override fun onH264Received(bytes: ByteArray, paySize: Int) {
                //视频数据
                if (mFPVVideoClient != null) {
                    mFPVVideoClient?.received(bytes, 4, paySize)
                }
            }

            override fun onGPSReceived(bytes: ByteArray) {
                //GPS数据
            }

            override fun onDataReceived(bytes: ByteArray) {
                //数传数据
            }

            override fun onDebugReceived(bytes: ByteArray) {
                //遥控器数据
            }
        })

        //渲染视频相关
        mFPVVideoClient = FPVVideoClient()
        mFPVVideoClient?.setDelegate(object : FPVVideoClient.Delegate {
            override fun onStopRecordListener(fileName: String) {
                //停止录像回调
            }

            override fun onSnapshotListener(fileName: String) {
                //拍照回调
            }

            //视频相关
            override fun renderI420(frame: ByteArray, width: Int, height: Int) {
                mPreviewDualVideoView?.renderI420(frame, width, height)
            }

            override fun setVideoSize(picWidth: Int, picHeight: Int) {
                mPreviewDualVideoView?.setVideoSize(picWidth, picHeight, mainHanlder)
            }

            override fun resetView() {
                mPreviewDualVideoView?.resetView(mainHanlder)
            }
        })

        //FPV控制
        mUsbSerialControl = UsbSerialControl(mUsbSerialConnection)
        mUSBMonitor = USBMonitor(mContext, mOnDeviceConnectListener)
        val deviceFilters = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter)
        mUSBMonitor?.setDeviceFilter(deviceFilters)
        mUSBMonitor?.register()
    }

    //使用 USBMonitor 处理USB连接回调
    private val mOnDeviceConnectListener: OnDeviceConnectListener = object : OnDeviceConnectListener {
        // USB device attach
        // USB设备插入
        override fun onAttach(device: UsbDevice) {
            if (deviceHasConnected(device) || mUsbDevice != null) {
                return
            }
            mContext.runOnUiThread(Runnable {
                try {
                    if (device == null) {
                        val devices = mUSBMonitor?.deviceList
                        if (devices?.size == 1) {
                            mUSBMonitor?.requestPermission(devices[0])
                        }
                    } else {
                        mUSBMonitor?.requestPermission(device)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            })
        }

        // USB device detach
        // USB设备物理断开
        override fun onDettach(device: UsbDevice) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return
            }
            if (!deviceHasConnected(device)) {
                return
            }
            disconnected()
        }

        // USB device has obtained permission
        // USB设备获得权限
        override fun onConnect(device: UsbDevice, var2: UsbControlBlock, var3: Boolean) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return
            }
            if (deviceHasConnected(device)) {
                return
            }
            synchronized(this) {
                if (BusinessUtils.deviceIsUartVideoDevice(device)) {
                    try {
                        //打开串口
                        mUsbSerialConnection.openConnection(device)
                        mUsbDevice = device
                        //开始渲染视频
                        mFPVVideoClient?.startPlayback()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        // USB device disconnected
        // USB设备关闭连接
        override fun onDisconnect(device: UsbDevice, var2: UsbControlBlock) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return
            }
            if (!deviceHasConnected(device)) {
                return
            }
            disconnected()
        }

        // USB device obtained permission failed
        // USB设备权限获取失败
        override fun onCancel() {}
    }

    //关闭连接
    fun disconnected() {
        if (mUsbSerialConnection != null) {
            try {
                mUsbSerialConnection.closeConnection()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
        if (mFPVVideoClient != null) {
            mFPVVideoClient?.stopPlayback()
        }
        mUsbDevice = null
        if (mUSBMonitor != null) {
            mUSBMonitor!!.unregister()
            mUSBMonitor!!.destroy()
            mUSBMonitor = null
        }
    }

    private fun deviceHasConnected(usbDevice: UsbDevice?): Boolean {
        return usbDevice != null && usbDevice === mUsbDevice
    }

}