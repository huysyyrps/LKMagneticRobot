package com.example.lkmagneticrobot.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.lkmagneticrobot.R
import java.lang.annotation.Documented
import kotlin.math.roundToInt


/**
 * 抬升高度
 */
class HeightView constructor(context: Context?, attrs: AttributeSet? = null) : View(context, attrs) {
    private var percent = 0
    var paintFill = Paint()
    var paintPowerBody = Paint()
    var paintPowerHeader = Paint()
    var paintText = Paint()
    var heightColor = R.color.red

    fun HeightView() {
        paintFill.isAntiAlias = true//是否抗锯齿
        paintFill.style = Paint.Style.FILL// 描边填充效果 1.STROKE 描边 2.FIll 填充 3.FILL_AND_STROKE 描边+填充

        paintPowerBody.isAntiAlias = true
        paintPowerBody.style = Paint.Style.FILL
        paintPowerBody.strokeWidth = dip2px(1.0f).toFloat()

        paintPowerBody.color = resources.getColor(R.color.theme_color,context?.theme)
        paintPowerHeader.isAntiAlias = true
        paintPowerHeader.style = Paint.Style.FILL
        paintPowerHeader.color = resources.getColor(R.color.theme_color,context?.theme)

        paintText.isAntiAlias = true
        paintText.style = Paint.Style.FILL
        paintText.color = resources.getColor(R.color.white)

        val dm = resources.displayMetrics
        val mScreenWidth = dm.widthPixels
        val mScreenHeight = dm.heightPixels
        val ratioWidth = 60F
        val ratioHeight = mScreenHeight-100F
        val ratioMetrics = Math.min(ratioWidth, ratioHeight)
        val textSize = (25.0f * ratioMetrics).roundToInt()
        paintText.textSize = textSize.toFloat()
    }

    private fun dip2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    // android.view.View
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paintFill.color = resources.getColor(R.color.theme_color)
//        paintFill.color = resources.getColor(heightColor)
        paintFill.color = resources.getColor(R.color.red,context?.theme)
//        paintPowerBody.color = resources.getColor(R.color.red)
        val a = width - dip2px(2.0f)
        val b = height - dip2px(3.0f)
        val left = dip2px(0.5f).toFloat()
        val top = dip2px(0.5f).toFloat()
        val right = dip2px(2.5f).toFloat()
        val bottom = dip2px(0.5f).toFloat()
        val textRectF = RectF(10.0f, 2.4f, a.toFloat(), b.toFloat()*(100-percent)/100)
        val powerBodyRectF = RectF(10.0f, 2.0f, a.toFloat(), (b.toFloat()))
        canvas.drawRoundRect(powerBodyRectF, 18.0f, 18.0f, paintPowerBody)
        canvas.drawRoundRect(textRectF,18.0f, 18.0f, paintFill)
//        canvas.drawText(percent.toString(), 100F, 100F, paintText)
    }

    @Synchronized
    fun setHeight(percent: Int) {
        this.percent = percent
        postInvalidate()
    }
}