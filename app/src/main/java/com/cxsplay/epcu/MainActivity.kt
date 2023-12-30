package com.cxsplay.epcu

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.bumptech.glide.Glide
import com.cxsplay.epcu.adapter.DeviceRvAdapter
import com.cxsplay.epcu.bean.BtDeviceBean
import com.cxsplay.epcu.bt.BluetoothChatService
import com.cxsplay.epcu.bt.BluetoothMatcher
import com.cxsplay.epcu.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bind: ActivityMainBinding

    private val mAdapter by lazy { DeviceRvAdapter() }

    private val bluetoothService: BluetoothChatService by lazy {
        BluetoothChatService(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    // 连接状态
                    BluetoothChatService.MSG_STATE_CHANGE -> doMsgStateChange(msg.arg1)
                    // 读取消息
                    BluetoothChatService.MSG_READ -> doMsgRead(msg.arg1)
                    // 写入消息
                    BluetoothChatService.MSG_WRITE -> doMsgWrite(msg.obj)
                    // 设备名称
                    BluetoothChatService.MSG_DEVICE_NAME -> ToastUtils.showShort("${msg.data}")
                    // 提示性消息
                    BluetoothChatService.MSG_TOAST -> ToastUtils.showShort("${msg.data}")
                }
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bind.root)
        initData()
    }

    @SuppressLint("MissingPermission")
    private fun initData() {

        lifecycleScope.launch {
            BluetoothMatcher.deviceFlow.collect {
                val bean = BtDeviceBean().apply {
                    this.name = it.name
                    this.address = it.address
                }
                mAdapter.addData(bean)
            }
        }
        bind.btnEnable.setOnClickListener { BluetoothMatcher.startSearching() }
        bind.rv.layoutManager = LinearLayoutManager(this)
        bind.rv.adapter = mAdapter
        mAdapter.itemClick { _, p ->
            val address = mAdapter.dataSet[p].address ?: return@itemClick
            initDevice2(address)
        }
        bind.btnPrintTest.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                bluetoothService.write("dddd\ndddd\ndddd\ndddd\n".toByteArray())
            }
        }
    }

    private fun initDevice2(address: String) {
        val device = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address)
        bluetoothService.connect(device)
    }

    private fun doMsgStateChange(state: Int) {
        when (state) {
            BluetoothChatService.STATE_NONE -> LogUtils.d("---STATE_NONE--->")
            BluetoothChatService.STATE_LISTEN -> LogUtils.d("---STATE_LISTEN--->")
            BluetoothChatService.STATE_CONNECTING -> LogUtils.d("---STATE_CONNECTING--->")
            BluetoothChatService.STATE_CONNECTED -> LogUtils.d("---STATE_CONNECTED--->")
        }
    }

    private fun doMsgRead(arg: Int) {
        ToastUtils.showShort("---msg_read-->$arg")
        LogUtils.d("---msg_read-->$arg")
    }

    private fun doMsgWrite(obj: Any) {
        if (obj !is ByteArray) return
        val content = obj.toString(Charsets.UTF_8)
        ToastUtils.showShort("---msg_write-->$content")
        LogUtils.d("---msg_write-->$content")
    }

}