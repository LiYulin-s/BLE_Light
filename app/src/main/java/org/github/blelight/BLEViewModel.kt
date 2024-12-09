/*
author: LiYulin-s
license: GPLv3
time: CST 2024/9/12
 */

package org.github.blelight

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.bluetooth.BluetoothDevice
import androidx.bluetooth.BluetoothLe
import androidx.bluetooth.ScanFilter
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class BLEViewModel(context: Context) : ViewModel() {

    // 数据类表示颜色
    data class Color(val r: Int, val g: Int, val b: Int)

    // 状态和数据流
    private val shouldWriteFlow = MutableStateFlow(Color(0, 0, 0))
    private val bluetoothLe = BluetoothLe(context)
    private val appContext = context
    private lateinit var device: BluetoothDevice

    // 连接状态
    val connectionStatus = mutableStateOf("Disconnected")

    init {
        // 初始化时启动扫描和连接
        scan()
    }


    fun scan() {
        connectionStatus.value = "Connecting"
        viewModelScope.launch {
            try {
                getGattList()
                connect()
            } catch (e: Exception) {
                Log.e("BLE", "Error during initialization: ${e.message}")
                connectionStatus.value = "Disconnected"
            }
        }
    }

    /**
     * 获取设备的 GATT 列表。
     */
    @SuppressLint("MissingPermission")
    private suspend fun getGattList() {
        // 检查权限
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            connectionStatus.value = "Permission Denied"
            return
        }
        val scanFlow = bluetoothLe.scan(
            listOf(ScanFilter(deviceName = "ESP32_Light"))
        )
        device = scanFlow.first().device
        Log.d("BLE", "Found device: $device")
    }

    /**
     * 向设备发送 RGB 数据。
     */
    fun sendData(r: Int, g: Int, b: Int) {
        Log.d("BLE", "Receiving color data: R=$r, G=$g, B=$b")
        runBlocking {
            shouldWriteFlow.emit(Color(r, g, b))
            Log.d("BLE", "Sent color data: R=$r, G=$g, B=$b")
        }
    }

    /**
     * 连接到蓝牙设备。
     */
    @SuppressLint("MissingPermission")
    private suspend fun connect() {
        // 检查权限
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            connectionStatus.value = "Permission Denied"
            return
        }

        // 连接到设备

        bluetoothLe.connectGatt(device) {
            connectionStatus.value = "Connected"
            try {
                shouldWriteFlow.collectLatest { shouldWrite ->
                    val serviceUUID = UUID.fromString("0000181F-0000-1000-8000-00805F9B34FB")
                    val characteristicUUID = UUID.fromString("0000290B-0000-1000-8000-00805F9B34FB")

                    // 获取服务
                    val service = getService(serviceUUID)
                    if (service == null) {
                        Log.d("BLE", "Service not found")
                        connectionStatus.value = "Disconnected"
                        return@collectLatest
                    }

                    // 获取特征
                    val characteristic = service.getCharacteristic(characteristicUUID)
                    if (characteristic == null) {
                        Log.d("BLE", "Characteristic not found")
                        return@collectLatest
                    }

                    // 写入特征
                    writeCharacteristic(
                        characteristic,
                        byteArrayOf(
                            shouldWrite.r.toByte(),
                            shouldWrite.g.toByte(),
                            shouldWrite.b.toByte()
                        )
                    )
                    Log.d("BLE", "Color changed to: $shouldWrite")
                }
            } catch (e: Exception) {
                Log.e("BLE", "Error while handling data: ${e.message}")
            }
        }
        connectionStatus.value = "Connecting"
    }

    /**
     * 检查是否具有指定权限。
     */
    private fun hasPermission(permission: String): Boolean {
        return ActivityCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
    }
}
