package com.example.lkmagneticrobot.view

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.lkmagneticrobot.MyApplication
import com.example.lkmagneticrobot.R


class BaseTitle:LinearLayout {
    private var attrs: AttributeSet? = null
    private var tviewTittle //标题
            : TextView? = null
    private var tvTitle //标题
            : String? = null
    private var ivviewLeft //图片
            : ImageView? = null
    private var isTittle //标题是否显示
            = false
    private var isleftIv //图标是否显示
            = false
    constructor(context: Context) : super(context) {
        initAttributes();
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        this.attrs=attrs;
        initAttributes();
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        this.attrs=attrs;
        initAttributes();
    }

    /**
     * 初始化属性
     */
    private fun initAttributes() {
        if (attrs != null) {
            val typedArray: TypedArray = MyApplication.context.obtainStyledAttributes(attrs, R.styleable.BaseTitle)
            if (typedArray != null) {
                tvTitle = typedArray.getString(R.styleable.BaseTitle_text_header_title)
                isTittle = typedArray.getBoolean(R.styleable.BaseTitle_text_header_is_title_visiable, true)
                isleftIv = typedArray.getBoolean(R.styleable.BaseTitle_text_header_is_left_iv_visiable, true)
                typedArray.recycle()
            }
        }
        initView()
    }

    /**
     * 初始化view
     */
    private fun initView() {
        LayoutInflater.from(MyApplication.context).inflate(R.layout.base_title, this, true)
        tviewTittle = findViewById<View>(R.id.tvHeaderTitle) as TextView
        ivviewLeft = findViewById<View>(R.id.ivHeaderTitle) as ImageView
        if (isTittle) {
            tviewTittle?.visibility = VISIBLE
            tviewTittle?.text = tvTitle
        } else {
            tviewTittle?.visibility = GONE
        }
        if (isleftIv) {
            ivviewLeft?.visibility = VISIBLE
        } else {
            ivviewLeft?.visibility = GONE
        }
    }
}