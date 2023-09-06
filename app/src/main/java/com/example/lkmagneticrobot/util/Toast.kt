package com.example.lkmagneticrobot.util

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun String.showToast(context: Context, duration:Int = Toast.LENGTH_LONG){
    val toastData = this
    CoroutineScope(Dispatchers.Main).launch  {
        Toast.makeText(context,toastData,duration).show()
    }
}

fun Int.showToast(context: Context, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(context, this, duration).show()
}