package com.example.lkmagneticrobot.util.dialog

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.example.lkmagneticrobot.activity.MainActivity
import com.example.lkmagneticrobot.util.PermissionallBack
import com.permissionx.guolindev.PermissionX
import java.util.ArrayList


class DialogUtil {
    //初始化重新扫描扫描dialog
    private lateinit var dialog: MaterialDialog

    /**
    权限申请
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun requestPermission(activity: AppCompatActivity, callBacl: PermissionallBack): Boolean {

        var permissionTag = false
        val requestList = ArrayList<String>()
        requestList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        requestList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        requestList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        requestList.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (requestList.isNotEmpty()) {
            PermissionX.init(activity)
                .permissions(requestList)
                .onExplainRequestReason { scope, deniedList ->
                    val message = "需要您同意以下权限才能正常使用"
                    callBacl.permissionState(false)
                    scope.showRequestReasonDialog(deniedList, message, "同意", "取消")
                }
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
                        Log.e("TAG", "所有申请的权限都已通过")
                        permissionTag = true
                        callBacl.permissionState(true)
                    } else {
                        callBacl.permissionState(false)
                        Log.e("TAG", "您拒绝了如下权限：$deniedList")
                        activity.finish()
                    }
                }
        }
        return permissionTag
    }
}