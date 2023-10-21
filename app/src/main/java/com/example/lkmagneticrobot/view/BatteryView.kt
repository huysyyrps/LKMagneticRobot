package com.example.lkmagneticrobot.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.internal.view.SupportMenu
import com.example.lkmagneticrobot.R


/**
 * 电量显示
 */
 class BatteryView constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    private var percent = 0
    private var protectElectQuantity = 0
    var paintFill = Paint()
    var paintPowerBody = Paint()
    var paintPowerHeader = Paint()
    var paintText = Paint()

    fun BatteryView() {
        paintFill.isAntiAlias = true//是否抗锯齿
        paintFill.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充

        paintPowerBody.isAntiAlias = true
        paintPowerBody.style = Paint.Style.STROKE
        paintPowerBody.strokeWidth = dip2px(1.0f).toFloat()
        paintPowerBody.color = resources.getColor(R.color.theme_color)

        paintPowerHeader.isAntiAlias = true
        paintPowerHeader.style = Paint.Style.FILL
        paintPowerHeader.color = resources.getColor(R.color.theme_color)

        paintText.isAntiAlias = true
        paintText.style = Paint.Style.FILL
        paintText.color = resources.getColor(R.color.holo_orange_light)

        val dm = resources.displayMetrics
        val mScreenWidth = dm.widthPixels
        val mScreenHeight = dm.heightPixels
        val ratioWidth = mScreenWidth / 720.0f
        val ratioHeight = mScreenHeight / 1080.0f
        val ratioMetrics = Math.min(ratioWidth, ratioHeight)
        val textSize = Math.round(25.0f * ratioMetrics)
        paintText.textSize = textSize.toFloat()
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    // android.view.View
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (percent > 30) {
            paintFill.color = resources.getColor(R.color.text_green)
        } else {
            paintFill.color = resources.getColor(R.color.red)
        }
        val a = width - dip2px(2.0f)
        val b = height - dip2px(3.0f)
        val left = dip2px(0.5f).toFloat()
        val top = dip2px(0.5f).toFloat()
        val right = dip2px(2.5f).toFloat()
        val bottom = dip2px(0.5f).toFloat()
        val textRectF = RectF(left, top, (a - right)/100*percent, b.toFloat())
        val powerBodyRectF = RectF(0.0f, 0.0f, a - right, b.toFloat())
        val powerHeaderRectF = RectF(a - right, (b / 5).toFloat(), a.toFloat(), b + bottom - b / 5)
        canvas.drawRect(textRectF, paintFill)
        canvas.drawRoundRect(powerBodyRectF, 8.0f, 8.0f, paintPowerBody)
        canvas.drawRect(powerHeaderRectF, paintPowerHeader)
        canvas.drawText(percent .toString(), (width / 2 - dip2px(6.5f)).toFloat(), (height - height * 2.3 / 5.5).toFloat(), paintText)
    }

    @Synchronized
    fun setProgress(percent: Int, protectElectQuantity: Int) {
        this.percent = percent
        this.protectElectQuantity = protectElectQuantity
        postInvalidate()
    }
}