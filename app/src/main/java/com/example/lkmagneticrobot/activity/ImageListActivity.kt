package com.example.lkmagneticrobot.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.adapter.ImageListAdapter
import com.example.lkmagneticrobot.constant.BaseActivity
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.util.AdapterPositionCallBack
import com.example.lkmagneticrobot.util.FileGet.listFileSortByModifyTime
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import kotlinx.android.synthetic.main.activity_image_list.*
import kotlinx.android.synthetic.main.dialog_remove.*
import top.zibin.luban.Luban
import top.zibin.luban.OnCompressListener
import java.io.File

class ImageListActivity : BaseActivity() {
    var selectIndex = 0
     var fileList : MutableList<File> = mutableListOf()
    private lateinit var dialog: MaterialDialog
    private lateinit var adapter: ImageListAdapter

    companion object{
        fun actionStart(context: Context) {
            val intent = Intent(context, ImageListActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_list)
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager
        smartRefreshLayout.setRefreshHeader(ClassicsHeader(this))
        smartRefreshLayout.setRefreshFooter(ClassicsFooter(this)) //是否启用下拉刷新功能
        smartRefreshLayout.setOnRefreshListener {
            getFileList()
        }

        imageView.setOnLongClickListener {
            dialog = MaterialDialog(this).show {
                customView(    //自定义弹窗
                    viewRes = R.layout.dialog_remove,//自定义文件
                    dialogWrapContent = true,    //让自定义宽度生效
                    scrollable = true,            //让自定义宽高生效
                    noVerticalPadding = true    //让自定义高度生效
                )
                cornerRadius(16f)  //圆角
            }
            dialog.btnRemoveCancel.setOnClickListener {
                dialog.dismiss()
            }
            dialog.btnRemoveSure.setOnClickListener {
                if (selectIndex == 0) {
                    //如果只有一条数据删除后显示暂无数据图片，隐藏listview
                    if (fileList.size == 1) {
                        val file = fileList[selectIndex]
                        file.delete()
                        fileList.removeAt(selectIndex)
                        linNoData.visibility = View.VISIBLE
                        linData.visibility = View.GONE
                    } else {
                        val file = fileList[selectIndex]
                        file.delete()
                        fileList.removeAt(selectIndex)
                        adapter.notifyDataSetChanged()
//                        var path = "${fileList[selectIndex].path}"
                        setBitmap(fileList[selectIndex])
                    }
                } else if (selectIndex == fileList.size - 1) {
                    val file = fileList[selectIndex]
                    file.delete()
                    fileList.removeAt(selectIndex)
                    --selectIndex
                    adapter.setSelectIndex(selectIndex)
                    adapter.notifyDataSetChanged()
//                    var path = "${fileList[selectIndex].path}"
                    setBitmap(fileList[selectIndex])
                } else if (selectIndex < fileList.size - 1 && selectIndex > 0) {
                    val file = fileList[selectIndex]
                    file.delete()
                    fileList.removeAt(selectIndex)
                    --selectIndex
                    adapter.setSelectIndex(selectIndex)
                    adapter.notifyDataSetChanged()
//                    var path = fileList[selectIndex].path
                    setBitmap(fileList[selectIndex])
                }
                dialog.dismiss()
            }
            true
        }

        ivImagelistBack.setOnClickListener { finish() }
        getFileList()
    }

    //获取数据列表
    @SuppressLint("NotifyDataSetChanged")
    private fun getFileList() {
        /**将文件夹下所有文件名存入数组*/
        var backList = listFileSortByModifyTime(this.externalCacheDir.toString()+ "/" + Constant.SAVE_IMAGE_PATH + "/").reversed()
        if (!backList.isNullOrEmpty()){
            fileList = backList.toMutableList()
        }
        if (fileList.isNullOrEmpty()) {
            linNoData.visibility = View.VISIBLE
            linData.visibility = View.GONE
        } else {
            linNoData.visibility = View.GONE
            linData.visibility = View.VISIBLE
            adapter = ImageListAdapter(
                fileList,
                selectIndex,
                this,
                object : AdapterPositionCallBack {
                    override fun backPosition(index: Int) {
                        selectIndex = index
//                        var path = fileList[index]?.path
                        setBitmap(fileList[selectIndex])
                    }
                })
            recyclerView.adapter = adapter
//            var path = fileList[selectIndex].path
            setBitmap(fileList[selectIndex])
            adapter.notifyDataSetChanged()
            smartRefreshLayout.finishLoadMore()
            smartRefreshLayout.finishRefresh()
        }
    }

    private fun setBitmap(file: File) {
        Luban.with(this)
            .load(file)
            .ignoreBy(100)
            .filter { file ->
                !(TextUtils.isEmpty(file) || file.toLowerCase().endsWith(".gif"))
            }
            .setCompressListener(object : OnCompressListener {
                override fun onStart() {
                    // TODO 压缩开始前调用，可以在方法内启动 loading UI
                }

                override fun onSuccess(file: File) {
                    val bitmap = BitmapFactory.decodeFile(file.path)
                    imageView.setImageBitmap(bitmap)
//                    tvFileName.text = file.name
                }

                override fun onError(e: Throwable) {

                }
            }).launch()
    }
}