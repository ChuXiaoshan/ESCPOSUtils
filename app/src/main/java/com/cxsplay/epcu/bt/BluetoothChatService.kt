package com.cxsplay.epcu.bt

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothChatService(handler: Handler) {

    companion object {
        //debug
        const val TAG = "BluetoothChatService"
        var D = true

        const val NAME = "BluetoothChat"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

        //region 连接状态常量
        //
        const val STATE_NONE = 0

        // 监听连接
        const val STATE_LISTEN = 1

        // 连接中
        const val STATE_CONNECTING = 2

        // 已连接
        const val STATE_CONNECTED = 3
        //endregion

        //region 消息类型
        const val MSG_STATE_CHANGE = 1
        const val MSG_READ = 2
        const val MSG_WRITE = 3
        const val MSG_DEVICE_NAME = 4
        const val MSG_TOAST = 5
        //endregion
    }


    private var mAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private var mHandler: Handler = handler

    private var mAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null

    private var mState: Int = 0

    /**
     * 设置当前连接状态
     */
    @Synchronized
    private fun setState(state: Int) {
        if (D) Log.d(TAG, "---setState()--->$mState--->$state")
        mState = state
        mHandler.obtainMessage(MSG_STATE_CHANGE, state, -1).sendToTarget()
    }

    /**
     * 返回当前连接状态
     */
    fun getState() = mState

    /**
     * 开启服务
     */
    @Synchronized
    fun start() {
        if (D) Log.d(TAG, "---start--->")
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
        // 启动监听线程
        if (mAcceptThread == null) mAcceptThread = AcceptThread().apply { start() }
        setState(STATE_LISTEN)
    }

    /**
     * 启动一个线程去连接远程设备
     */
    @Synchronized
    fun connect(device: BluetoothDevice) {
        if (D) Log.d(TAG, "---connect_to--->$device")
        if (mState == STATE_CONNECTING) {
            mConnectThread?.cancel()
            mConnectThread = null
        }
        mConnectedThread?.cancel()
        mConnectedThread = null
        mConnectThread = ConnectThread(device).apply { start() }
        setState(STATE_CONNECTING)
    }

    @Synchronized
    fun connected(socket: BluetoothSocket?, device: BluetoothDevice?) {
        if (D) Log.d(TAG, "---connected--->")
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
        mAcceptThread?.cancel()
        mAcceptThread = null
        // 启动线程来管理连接并传输数据
        socket?.run { mConnectedThread = ConnectedThread(this).apply { start() } }
        // 通知调用者设备已连接
        val msg = mHandler.obtainMessage(MSG_DEVICE_NAME)
        msg.data = Bundle().apply { putString("DEVICE_NAME", device?.name ?: "") }
        mHandler.sendMessage(msg)
        setState(STATE_CONNECTED)
    }

    /**
     * 取消所有线程
     */
    @Synchronized
    fun stop() {
        mConnectThread?.cancel()
        mConnectThread = null
        mConnectedThread?.cancel()
        mConnectedThread = null
        mAcceptThread?.cancel()
        mAcceptThread = null
        setState(STATE_NONE)
    }

    /**
     * 以非同步的方式写入数据到ConnectedThread
     */
    fun write(out: ByteArray) {
        val r: ConnectedThread? = synchronized(this) {
            if (mState != STATE_CONNECTED) return
            mConnectedThread
        }
        // 执行不同步的写操作
        r?.write(out)
    }

    /**
     * 连接失败，通知调用者
     */
    private fun connectionFailed() {
        setState(STATE_LISTEN)
        val msg = mHandler.obtainMessage(MSG_TOAST)
        msg.data = Bundle().apply { putString("TOAST", "Unable to connect device") }
        mHandler.sendMessage(msg)
    }

    /**
     * 连接丢失，通知调用者
     */
    private fun connectionLost() {
        setState(STATE_LISTEN)
        val msg = mHandler.obtainMessage(MSG_TOAST)
        msg.data = Bundle().apply { putString("TOAST", "Device connection was lost") }
        mHandler.sendMessage(msg)
    }

    /**
     *
     */
    inner class AcceptThread : Thread() {

        // 本地 socket 服务
        private var mmServerSocket: BluetoothServerSocket? = null

        init {
            //mmServerSocket 初始化
            mmServerSocket = try {
                mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "---listen_failed--->", e)
                null
            }
        }

        override fun run() {
            if (D) Log.d(TAG, "---begin_mAcceptThread--->$this")
            name = "AcceptThread"
            var socket: BluetoothSocket? = null
            // 没有被连接就一直监听，直到连接
            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "---accept()_failed--->", e)
                    break
                }
                socket?.run {
                    synchronized(this@BluetoothChatService) {
                        when (mState) {
                            STATE_LISTEN,
                            STATE_CONNECTING -> {
                                connected(socket, socket.remoteDevice)
                            }

                            STATE_NONE,
                            STATE_CONNECTED -> {
                                try {
                                    socket.close()
                                } catch (e: IOException) {
                                    Log.e(TAG, "---Could not close unwanted socket--->", e)
                                }
                            }

                            else -> {}
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "---END mAcceptThread--->")
        }

        fun cancel() {
            if (D) Log.d(TAG, "---cancel--->$this")
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "---close() of server failed--->", e)
            }
        }
    }

    /**
     * 连接设备线程
     */
    inner class ConnectThread(private var mmDevice: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocket? = null

        init {
            mmSocket = try {
                mmDevice.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e(TAG, "---create() failed--->", e)
                null
            }
        }

        override fun run() {
            Log.i(TAG, "---BEGIN mConnectThread--->")
            name = "ConnectThread"
            mAdapter.cancelDiscovery()
            //建立连接
            try {
                Log.i(TAG, "---BEGIN mConnectThread1--->")
                mmSocket?.connect()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.i(TAG, "---BEGIN mConnectThread2--->")
                connectionFailed()
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e(TAG, "---unable to close() socket during connection failure--->", e2)
                }
                //启动服务重新监听
                this@BluetoothChatService.start()
                return
            }
            Log.i(TAG, "---BEGIN mConnectThread3--->")
            // 连接完成，重置连接线程
            synchronized(this@BluetoothChatService) {
                mConnectThread = null
            }
            Log.i(TAG, "---BEGIN mConnectThread4--->")
            connected(mmSocket, mmDevice)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }
        }
    }

    inner class ConnectedThread(private var mmSocket: BluetoothSocket) : Thread() {

        private var mmInStream: InputStream? = null
        private var mmOutStream: OutputStream? = null

        init {
            Log.d(TAG, "---create ConnectedThread--->")
            mmInStream = try {
                mmSocket.inputStream
            } catch (e: IOException) {
                Log.e(TAG, "---temp sockets not created--->", e)
                null
            }
            mmOutStream = try {
                mmSocket.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "---temp sockets not created--->", e)
                null
            }
        }

        override fun run() {
            Log.i(TAG, "---BEGIN mConnectedThread--->")
            val buffer = ByteArray(1024)
            var bytes: Int
            while (true) {
                try {
                    bytes = mmInStream?.read(buffer) ?: 0
                    mHandler.obtainMessage(MSG_READ, bytes, -1, buffer).sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "---disconnected--->", e)
                    connectionLost()
                    break
                }
            }
        }

        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)
                mHandler.obtainMessage(MSG_WRITE, -1, -1, buffer).sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "---close() of connect socket failed--->", e)
            }
        }
    }
}