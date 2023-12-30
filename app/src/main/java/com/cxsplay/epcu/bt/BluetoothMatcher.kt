package com.cxsplay.epcu.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.Utils
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object BluetoothMatcher {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    val deviceFlow: Flow<BluetoothDevice> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                LogUtils.d("---action--->${intent?.action}")
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        if (isPrinter(device)) trySendBlocking(device!!)
                    }
                }
            }
        }
        Utils.getApp().registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        LogUtils.d("---deviceFlow--->")
        awaitClose {
            LogUtils.d("---awaitClose--->")
            Utils.getApp().unregisterReceiver(receiver)
        }
    }

    fun isPrinter(device: BluetoothDevice?): Boolean {
        val major = device?.bluetoothClass?.majorDeviceClass
        return (device != null && !device.name.isNullOrEmpty())
                && (major == BluetoothClass.Device.Major.IMAGING
                || major == BluetoothClass.Device.Major.UNCATEGORIZED)
    }

    /**
     * 设备是否支持蓝牙
     */
    fun isSupport() = bluetoothAdapter != null

    /**
     * 开始搜索设备
     */
    fun startSearching() = requestPermission()

    /**
     * 获取已配对设备列表
     */
    fun getBondedDevices(): List<BluetoothDevice>? {
        return bluetoothAdapter?.bondedDevices?.toList()
    }

    /**
     *位置权限声明
     */
    private fun requestPermission() {
        PermissionUtils.permission(PermissionConstants.LOCATION)
            .callback(object : PermissionUtils.SimpleCallback {
                override fun onGranted() {
                    LogUtils.d("---onGranted--->")
                    startDiscovery()
                }

                override fun onDenied() {
                    LogUtils.e("Location Permission denied!")
                }
            }).request()
    }

    private fun startDiscovery() {
        if (bluetoothAdapter == null) {
            LogUtils.e("---Bluetooth is not supported on this hardware--->")
            return
        }
        if (!bluetoothAdapter.isEnabled) bluetoothAdapter.enable()
        bluetoothAdapter.startDiscovery()
    }
}