package com.example.lkmagneticrobot.util

import android.graphics.Color
import android.graphics.Paint

object BasePaint {
    private lateinit var linePaint: Paint
    private lateinit var textbgpaint: Paint
    private lateinit var textpaint: Paint
    fun getLinePaint(): Paint {
        linePaint = Paint()
        linePaint!!.style = Paint.Style.STROKE
        linePaint!!.strokeWidth = 2f
        linePaint!!.color = Color.RED
        return linePaint
    }

    fun getTextpaint(): Paint {
        textpaint = Paint()
        textpaint!!.color = Color.RED
        textpaint!!.textSize = 26f
        textpaint!!.strokeWidth = 2f
        textpaint!!.textAlign = Paint.Align.LEFT
        return textpaint
    }

    fun getTextbgpaint(): Paint {
        textbgpaint = Paint()
        textbgpaint!!.color = Color.WHITE
        textbgpaint!!.style = Paint.Style.FILL
        return textbgpaint
    }
}