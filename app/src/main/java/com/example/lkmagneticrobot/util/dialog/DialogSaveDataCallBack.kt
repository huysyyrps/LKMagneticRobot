package com.example.lkmagneticrobot.util.dialog

interface DialogSaveDataCallBack {
    fun cancelCallBack()
    fun sureCallBack(dataName:String)
}