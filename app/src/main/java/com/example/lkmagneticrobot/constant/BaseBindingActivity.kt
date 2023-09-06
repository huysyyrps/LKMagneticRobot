package com.example.lkmagneticrobot.constant

import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.dylanc.viewbinding.base.ActivityBinding
import com.dylanc.viewbinding.base.ActivityBindingDelegate
import com.example.lkmagneticrobot.util.ActivityCollector


abstract class BaseBindingActivity<VB : ViewBinding> : AppCompatActivity(), ActivityBinding<VB> by ActivityBindingDelegate() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("BaseActivity", javaClass.simpleName)
        hideStatusBar()
        ActivityCollector.addActivity(this)
        setContentViewWithBinding()

    }


    /**
     * 隐藏状态栏
     */
    private fun hideStatusBar() {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    override fun onDestroy() {
        super.onDestroy()
        ActivityCollector.removeActivity(this)
    }
}