package com.example.lkmagneticrobot.util

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.activity.ImageListActivity
import com.example.lkmagneticrobot.activity.MainActivity
import com.example.lkmagneticrobot.activity.VideoListActivity

object MainUi {
    @JvmStatic
    fun showPopupMenu(view: View?, tag: String?, context: Context) {
        // View当前PopupMenu显示的相对View的位置
        val popupMenu = PopupMenu(context, view)
        // menu布局
        popupMenu.menuInflater.inflate(R.menu.dialog, popupMenu.menu)
        // menu的item点击事件
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.title == "图片") {
                val intent = Intent(context, ImageListActivity::class.java)
                intent.putExtra("tag", tag)
                context.startActivity(intent)
            } else if (item.title == "视频") {
                val intent = Intent(context, VideoListActivity::class.java)
                intent.putExtra("tag", tag)
                context.startActivity(intent)
            }
            false
        }
        // PopupMenu关闭事件
        popupMenu.setOnDismissListener { }
        popupMenu.show()
    }


    fun showLightMenu(view: View?, context: Context, callBack: MenuCallBack) {
        // View当前PopupMenu显示的相对View的位置
        val popupMenu = PopupMenu(context, view)
        // menu布局
        popupMenu.menuInflater.inflate(R.menu.light, popupMenu.menu)
        // menu的item点击事件
        popupMenu.setOnMenuItemClickListener { item ->
            if (item.title == context.resources.getText(R.string.light_white)) {
                callBack.menuCallBack("白光")
                popupMenu.dismiss()
            } else if (item.title == context.resources.getText(R.string.light_black)) {
                callBack.menuCallBack("紫光")
                popupMenu.dismiss()
            }
            false
        }
        // PopupMenu关闭事件
        popupMenu.setOnDismissListener { }
        popupMenu.show()
    }
}