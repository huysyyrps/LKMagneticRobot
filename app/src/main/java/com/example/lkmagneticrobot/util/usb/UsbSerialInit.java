package com.example.lkmagneticrobot.util.usb;

import android.app.Activity;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;

import com.example.lkmagneticrobot.R;
import com.example.lkmagneticrobot.util.usbfpv.FPVVideoClient;
import com.example.lkmagneticrobot.util.usbfpv.GLHttpVideoSurface;
import com.skydroid.android.usbserial.DeviceFilter;
import com.skydroid.android.usbserial.USBMonitor;
import com.skydroid.fpvlibrary.usbserial.UsbSerialConnection;
import com.skydroid.fpvlibrary.usbserial.UsbSerialControl;
import com.skydroid.fpvlibrary.utils.BusinessUtils;

import java.util.List;

public class UsbSerialInit {
    private Activity mContext;
    //Usb监视器
    private USBMonitor mUSBMonitor;
    //Usb设备
    private UsbDevice mUsbDevice;
    private GLHttpVideoSurface mPreviewDualVideoView;
    //视频渲染
    private FPVVideoClient mFPVVideoClient;
    //usb连接实例
    private UsbSerialConnection mUsbSerialConnection;
    //摄像头控制
    private UsbSerialControl mUsbSerialControl;
    private Handler mainHanlder = new Handler(Looper.getMainLooper());

    public void init(Activity mContext, GLHttpVideoSurface mPreviewDualVideoView){
        this.mContext = mContext;
        this.mPreviewDualVideoView = mPreviewDualVideoView;
        //初始化usb连接
        mUsbSerialConnection = new UsbSerialConnection(mContext);
        mUsbSerialConnection.setDelegate(new UsbSerialConnection.Delegate() {
            @Override
            public void onH264Received(byte[] bytes, int paySize) {
                //视频数据
                if(mFPVVideoClient != null){
                    mFPVVideoClient.received(bytes,4,paySize);
                }
            }

            @Override
            public void onGPSReceived(byte[] bytes) {
                //GPS数据
            }

            @Override
            public void onDataReceived(byte[] bytes) {
                //数传数据
            }

            @Override
            public void onDebugReceived(byte[] bytes) {
                //遥控器数据
            }
        });

        //渲染视频相关
        mFPVVideoClient = new FPVVideoClient();
        mFPVVideoClient.setDelegate(new FPVVideoClient.Delegate() {
            @Override
            public void onStopRecordListener(String fileName) {
                //停止录像回调
            }

            @Override
            public void onSnapshotListener(String fileName) {
                //拍照回调
            }

            //视频相关
            @Override
            public void renderI420(byte[] frame, int width, int height) {
//                UsbSerialInit.this.mPreviewDualVideoView.renderI420(frame, 1080,1080);
                UsbSerialInit.this.mPreviewDualVideoView.renderI420(frame,width,height);
            }

            @Override
            public void setVideoSize(int picWidth, int picHeight) {
                UsbSerialInit.this.mPreviewDualVideoView.setVideoSize(picWidth,picHeight,mainHanlder);
//                UsbSerialInit.this.mPreviewDualVideoView.setVideoSize(1080,1080,mainHanlder);
            }

            @Override
            public void resetView() {
                UsbSerialInit.this.mPreviewDualVideoView.resetView(mainHanlder);
            }
        });

        //FPV控制
        mUsbSerialControl = new UsbSerialControl(mUsbSerialConnection);
        mUSBMonitor = new USBMonitor(mContext,mOnDeviceConnectListener);
        List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(mContext, R.xml.device_filter);
        mUSBMonitor.setDeviceFilter(deviceFilters);
        mUSBMonitor.register();

    }

    //使用 USBMonitor 处理USB连接回调
    private USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        // USB device attach
        // USB设备插入
        @Override
        public void onAttach(final UsbDevice device) {
            if(deviceHasConnected(device) || mUsbDevice != null){
                return;
            }
            mContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(device == null){
                            List<UsbDevice> devices = mUSBMonitor.getDeviceList();
                            if(devices.size() == 1){
                                mUSBMonitor.requestPermission(devices.get(0));
                            }
                        }else {
                            mUSBMonitor.requestPermission(device);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        // USB device detach
        // USB设备物理断开
        @Override
        public void onDettach(UsbDevice device) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (!deviceHasConnected(device)) {
                return;
            }
            disconnected();
        }

        // USB device has obtained permission
        // USB设备获得权限
        @Override
        public void onConnect(UsbDevice device, USBMonitor.UsbControlBlock var2, boolean var3) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (deviceHasConnected(device)) {
                return;
            }
            synchronized (this){
                if (BusinessUtils.deviceIsUartVideoDevice(device)) {
                    try {
                        //打开串口
                        mUsbSerialConnection.openConnection(device);
                        mUsbDevice = device;
                        //开始渲染视频
                        mFPVVideoClient.startPlayback();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }

        // USB device disconnected
        // USB设备关闭连接
        @Override
        public void onDisconnect(UsbDevice device, USBMonitor.UsbControlBlock var2) {
            if (!BusinessUtils.deviceIsUartVideoDevice(device)) {
                return;
            }
            if (!deviceHasConnected(device)) {
                return;
            }
            disconnected();
        }

        // USB device obtained permission failed
        // USB设备权限获取失败
        @Override
        public void onCancel() {

        }
    };

    //关闭连接
    public void disconnected(){
        if(mUsbSerialConnection != null){
            try {
                mUsbSerialConnection.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if(mFPVVideoClient != null){
            mFPVVideoClient.stopPlayback();
        }
        mUsbDevice = null;
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
    }

    public boolean deviceHasConnected(UsbDevice usbDevice){
        return usbDevice != null && usbDevice == mUsbDevice;
    }
}
