package com.example.lkmagneticrobot.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.lkmagneticrobot.MyApplication
import com.example.lkmagneticrobot.R

/**
 * 自定义圆角类按钮
 */
class BaseButton : AppCompatButton {
    constructor(context: Context) : super(context) {
        initView(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(context, attrs)

    }

    private fun initView(context: Context, attrs: AttributeSet?) {
        //获取默认的颜色值 如果按钮没有设置颜色值 默认为这个颜色
        val default = ContextCompat.getColor(context, R.color.theme_color)
        //获取自定义的属性值
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.BaseButton)
        //获取设置的背景色
        val bgColor = typedArray.getColor(R.styleable.BaseButton_bg_color, default)
        //获取设置的圆角大小
        val buttonCorner = typedArray.getDimensionPixelSize(R.styleable.BaseButton_bg_corner, 0)
        //设置按钮是否可以点击
        val buttonclient = typedArray.getBoolean(R.styleable.BaseButton_bg_client, true)

        //生成圆角图片
        val bgcDrawable = GradientDrawable()
        //设置图片颜色
        bgcDrawable.setColor(bgColor)
        //设置圆角大小
        bgcDrawable.cornerRadius = buttonCorner.toFloat()

        //生成一张半透明的灰色图片 #31000000为遮罩颜色 可自定义
        val bgcDrawable1 = GradientDrawable()
        bgcDrawable1.setColor(MyApplication.context.getColor(R.color.style_red))
        bgcDrawable1.cornerRadius = buttonCorner.toFloat()

        //生成一个图层叠加的图片 上面用灰色盖住 模拟变暗效果
        val arr = arrayOf(bgcDrawable, bgcDrawable1)
        val layerDrawable = LayerDrawable(arr)

        //设置点击后 变暗效果
        val stateListDrawable = StateListDrawable()
        if (buttonclient){
            stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), layerDrawable)
        }
        stateListDrawable.addState(intArrayOf(), bgcDrawable)
        background = stateListDrawable
        typedArray.recycle()
    }

}