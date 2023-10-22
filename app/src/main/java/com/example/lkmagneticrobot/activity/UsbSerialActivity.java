package com.example.lkmagneticrobot.activity;

import static com.littlegreens.netty.client.status.ConnectState.STATUS_CONNECT_CLOSED;
import static com.littlegreens.netty.client.status.ConnectState.STATUS_CONNECT_ERROR;
import static com.littlegreens.netty.client.status.ConnectState.STATUS_CONNECT_SUCCESS;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.lkmagneticrobot.R;
import com.example.lkmagneticrobot.constant.Constant;
import com.example.lkmagneticrobot.util.MainUi;
import com.example.lkmagneticrobot.util.Netty.BaseTcpClient;
import com.example.lkmagneticrobot.util.Netty.NettyTcpClient;
import com.example.lkmagneticrobot.util.PermissionallBack;
import com.example.lkmagneticrobot.util.dialog.DialogUtil;
import com.example.lkmagneticrobot.util.mediaprojection.MediaUtil;
import com.example.lkmagneticrobot.util.usb.UsbSerialInit;
import com.example.lkmagneticrobot.util.usbfpv.GLHttpVideoSurface;
import com.example.lkmagneticrobot.view.BaseButton;
import com.littlegreens.netty.client.listener.NettyClientListener;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * usb串口连接方式
 */
public class UsbSerialActivity extends AppCompatActivity implements NettyClientListener<String> {
    @BindView(R.id.fPVVideoView)
    GLHttpVideoSurface fPVVideoView;
    @BindView(R.id.imageView)
    ImageView imageView;
    @BindView(R.id.frameLayout)
    FrameLayout frameLayout;
    @BindView(R.id.btnCamer)
    BaseButton btnCamer;
    @BindView(R.id.btnStartVideo)
    BaseButton btnStartVideo;
    @BindView(R.id.btnStopVideo)
    BaseButton btnStopVideo;
    @BindView(R.id.btnFile)
    BaseButton btnFile;
//    private GLHttpVideoSurface mPreviewDualVideoView;
    private MediaProjectionManager mediaManager;
    private MediaProjection mMediaProjection;
    Boolean runing = true;
    NettyTcpClient mNettyTcpClient;
    BaseTcpClient baseTcpClient;

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usbserial);
        ButterKnife.bind(this);
        initView();
        //是否通过全部权限
        new DialogUtil().requestPermission(this, new PermissionallBack() {
            @Override
            public void permissionState(boolean state) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (runing) {
                            getVideoPhoto();
                        }
                    }
                }).start();
            }
        });
        baseTcpClient = BaseTcpClient.getInstance();
        settingNetty();
    }

    //--------------tcp----------------
    private void settingNetty() {
        mNettyTcpClient = baseTcpClient.initTcpClient("192.168.144.101", 14551);
//        mNettyTcpClient = baseTcpClient.initTcpClient("172.16.20.5", 5000);
        mNettyTcpClient.setListener(this); //设置TCP监听
        baseTcpClient.tcpClientConntion(mNettyTcpClient);
    }
    private void initView() {
//        mPreviewDualVideoView = findViewById(R.id.fPVVideoView);
        fPVVideoView.init();
        new UsbSerialInit().init(this, fPVVideoView);
    }

    private void getVideoPhoto() {
        try {
            InputStream inputstream = null;
            //创建一个URL对象
            URL videoUrl = new URL(Constant.URL+"?action=snapshot");
            //利用HttpURLConnection对象从网络中获取网页数据
            HttpURLConnection conn = (HttpURLConnection) videoUrl.openConnection();
            //设置输入流
            //设置输入流
            conn.setDoInput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            //连接
            conn.connect();
            //得到网络返回的输入流
            inputstream = conn.getInputStream();
            //创建出一个bitmap
            Bitmap bmp = BitmapFactory.decodeStream(inputstream);
            imageView.setImageBitmap(bmp);
            //关闭HttpURLConnection连接
            conn.disconnect();
        }  catch (Exception ex) {
            ex.printStackTrace();
        }  finally {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @OnClick({R.id.fPVVideoView, R.id.imageView, R.id.btnCamer, R.id.btnStartVideo, R.id.btnStopVideo, R.id.btnFile})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fPVVideoView:
                ViewGroup.LayoutParams linearParams = imageView.getLayoutParams();
                linearParams.height = 280;
                linearParams.width = 308;
                imageView.bringToFront();
                imageView.setLayoutParams(linearParams);

                ViewGroup.LayoutParams linearParams1 = fPVVideoView.getLayoutParams();
                linearParams1.height = 1150;
                linearParams1.width = 1265;
                fPVVideoView.setLayoutParams(linearParams1);
                //.onSurfaceDestroyed();
                fPVVideoView.getRenderer().onSurfaceDestroyed();
                break;
            case R.id.imageView:
                ViewGroup.LayoutParams linearParams2 = fPVVideoView.getLayoutParams();
                linearParams2.height = 280;
                linearParams2.width = 308;
                fPVVideoView.bringToFront();
                new UsbSerialInit().disconnected();
                fPVVideoView.setLayoutParams(linearParams2);

                ViewGroup.LayoutParams linearParams3 = imageView.getLayoutParams();
                linearParams3.height = 1150;
                linearParams3.width = 1265;
                imageView.setLayoutParams(linearParams3);

                break;
            case R.id.btnCamer:
                mediaManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mMediaProjection == null) {
                    Intent captureIntent = mediaManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, Constant.TAG_ONE);
                } else {
                    MediaUtil.captureImages(this, mMediaProjection,"usb");
                }
                break;
            case R.id.btnStartVideo:
                mediaManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                if (mMediaProjection == null) {
                    Intent captureIntent = mediaManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, Constant.TAG_TWO);
                } else {
                    MediaUtil.startMedia(this ,mMediaProjection,"usb");
                    btnStartVideo.setVisibility(View.GONE);
                    btnStopVideo.setVisibility(View.VISIBLE);
                }
                break;
            case R.id.btnStopVideo:
                MediaUtil.stopMedia();
                btnStartVideo.setVisibility(View.VISIBLE);
                btnStopVideo.setVisibility(View.GONE);
                break;
            case R.id.btnFile:
                MainUi.showPopupMenu(btnFile, "Desc", this);
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode==Constant.TAG_ONE){
                mMediaProjection =  mediaManager.getMediaProjection(resultCode, data) ;
                MediaUtil.captureImages(this, mMediaProjection,"usb");
            }
            if (requestCode==Constant.TAG_TWO){
                mMediaProjection =  mediaManager.getMediaProjection(resultCode, data) ;
                MediaUtil.startMedia(this, mMediaProjection,"usb");
                btnStartVideo.setVisibility(View.GONE);
                btnStopVideo.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onMessageResponseClient(String msg, int index) {
        Log.e("XXX", msg);
    }

    @Override
    public void onClientStatusConnectChanged(int statusCode, int index) {
        if (statusCode == STATUS_CONNECT_SUCCESS) {
            Log.e("XXX", "成功");
        } else if (statusCode == STATUS_CONNECT_CLOSED) {
            Log.e("XXX", "断开");
        } else if (statusCode == STATUS_CONNECT_ERROR) {
            Log.e("XXX", "失败");
        }
    }
}
