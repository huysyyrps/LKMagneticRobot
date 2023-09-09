package com.example.lkmagneticrobot.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.example.lkmagneticrobot.R
import com.example.lkmagneticrobot.adapter.ImageListAdapter
import com.example.lkmagneticrobot.constant.BaseBindingActivity
import com.example.lkmagneticrobot.constant.Constant
import com.example.lkmagneticrobot.databinding.ActivityVideoListBinding
import com.example.lkmagneticrobot.util.AdapterPositionCallBack
import com.example.lkmagneticrobot.util.FileGet
import com.example.lkmagneticrobot.util.LogUtil
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.ClassicsHeader
import kotlinx.android.synthetic.main.dialog_remove.*
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate

class VideoListActivity : BaseBindingActivity<ActivityVideoListBinding>() {
    var selectIndex = 0
    private var longTime:Long? = 0
    private var longTimeCurrent:Long? = 0
    var timer:Timer? = null
    private lateinit var dialog: MaterialDialog
    lateinit var fileList : MutableList<File>
    private lateinit var adapter: ImageListAdapter

    companion object{
        fun actionStart(context: Context) {
            val intent = Intent(context, VideoListActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        binding.smartRefreshLayout.setRefreshHeader(ClassicsHeader(this))
        binding.smartRefreshLayout.setRefreshFooter(ClassicsFooter(this)) //是否启用下拉刷新功能
        binding.smartRefreshLayout.setOnRefreshListener {
            getFileList()
        }

        binding.imageView.setOnLongClickListener {
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
                        binding.linNoData.visibility = View.VISIBLE
                        binding.linData.visibility = View.GONE
                    } else {
                        val file = fileList[selectIndex]
                        file.delete()
                        fileList.removeAt(selectIndex)
                        adapter.notifyDataSetChanged()
//                        var path = "${fileList[selectIndex].path}"
                        setVideo(fileList[selectIndex])
                    }
                } else if (selectIndex == fileList.size - 1) {
                    val file = fileList[selectIndex]
                    file.delete()
                    fileList.removeAt(selectIndex)
                    --selectIndex
                    adapter.setSelectIndex(selectIndex)
                    adapter.notifyDataSetChanged()
//                    var path = "${fileList[selectIndex].path}"
                    setVideo(fileList[selectIndex])
                } else if (selectIndex < fileList.size - 1 && selectIndex > 0) {
                    val file = fileList[selectIndex]
                    file.delete()
                    fileList.removeAt(selectIndex)
                    --selectIndex
                    adapter.setSelectIndex(selectIndex)
                    adapter.notifyDataSetChanged()
//                    var path = fileList[selectIndex].path
                    setVideo(fileList[selectIndex])
                }
                dialog.dismiss()
            }
            true
        }

        binding.ivImagelistBack.setOnClickListener { finish() }
        binding.ivStart.setOnClickListener {
            binding.imageView.visibility = View.GONE
            binding.videoView.visibility = View.VISIBLE
            binding.videoView.start()
            longTimeCurrent = longTime
            timer = Timer()
            timer?.scheduleAtFixedRate(1000, 1000) {
                longTimeCurrent = longTimeCurrent?.minus(1000)
                binding.tvTime.text = longTimeCurrent?.let { timeParse(it) }
            }
        }
        binding.videoView.setOnPreparedListener(MediaPlayer.OnPreparedListener { binding.videoView.start() })
        binding.videoView.setOnCompletionListener(MediaPlayer.OnCompletionListener {
            binding.videoView.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
            binding.tvTime.text = longTime?.let { timeParse(it) }
            timer?.cancel()
        })
        binding.videoView.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
            LogUtil.e("TAG","2222")
            true
        })

        getFileList()
    }

    //获取数据列表
    private fun getFileList() {
        /**将文件夹下所有文件名存入数组*/
        fileList = FileGet.listFileSortByModifyTime(this.externalCacheDir.toString() + "/" + Constant.SAVE_VIDEO_PATH + "/").reversed() as MutableList<File>
        if (fileList.isNullOrEmpty()) {
            binding.linNoData.visibility = View.VISIBLE
            binding.linData.visibility = View.GONE
        } else {
            binding.linNoData.visibility = View.GONE
            binding.linData.visibility = View.VISIBLE
            adapter = ImageListAdapter(
                fileList,
                selectIndex,
                this,
                object : AdapterPositionCallBack {
                    override fun backPosition(index: Int) {
                        selectIndex = index
                        timer?.cancel()
                        binding.videoView.visibility = View.GONE
                        binding.imageView.visibility = View.VISIBLE
                        setVideo(fileList[selectIndex])
                    }
                })
            binding.recyclerView.adapter = adapter
            setVideo(fileList[selectIndex])
            adapter.notifyDataSetChanged()
            binding.smartRefreshLayout?.finishLoadMore()
            binding.smartRefreshLayout?.finishRefresh()
        }
    }

    private fun setVideo(file: File) {
        binding.imageView.setImageBitmap(getRingBitmap(file))
        binding.videoView.setVideoPath(file.path)
        binding.tvFileName.text = file.name
        longTime = getRingDuring(file)
        longTimeCurrent = longTime
        binding.tvTime.text = longTime?.let { timeParse(it) }
    }

    /**
     * 获取视频第一帧图片
     *
     * @param mUri
     * @return
     */
    fun getRingBitmap(mUri: File?): Bitmap? {
        var bitmap: Bitmap? = null
        val mmr = MediaMetadataRetriever()
        try {
            if (mUri != null) {
                mmr.setDataSource(mUri.absolutePath)
                //获得视频第一帧的Bitmap对象
                bitmap = mmr.getFrameAtTime((1000 * 5).toLong())
            }
        } catch (ex: Exception) {
            Log.e("XXX", ex.toString())
        } finally {
            mmr.release()
        }
        return bitmap
    }

    /**
     * 获取视频时长
     *
     * @param mUri
     * @return
     */
    fun getRingDuring(mUri: File?): Long? {
        var duration: String? = null
        val mmr = MediaMetadataRetriever()
        try {
            if (mUri != null) {
                mmr.setDataSource(mUri.absolutePath)
                duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            }
        } catch (ex: java.lang.Exception) {
            Log.e("XXX", ex.toString())
        } finally {
            mmr.release()
        }
        return duration?.toLong() ?: 0
    }

    /**
     * 将毫秒转换成分钟
     *
     * @param duration
     * @return
     */
    fun timeParse(duration: Long): String? {
        var time: String? = ""
        val minute = duration / 60000
        val seconds = duration % 60000
        val second = Math.round(seconds.toFloat() / 1000).toLong()
        if (minute < 10) {
            time += "0"
        }
        time += "$minute:"
        if (second < 10) {
            time += "0"
        }
        time += second
        return time
    }
}