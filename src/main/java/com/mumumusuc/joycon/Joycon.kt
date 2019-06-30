package com.mumumusuc.joycon

import android.content.res.AssetManager
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Joycon {
    enum class NsButton {
        Y, X, B, A, LSR, LSL, R, ZR, MINUS, PLUS, RS, LS, HOME, CAP, NOP, CG, DOWN, UP, RIGHT, LEFT, RSR, RSL, L, ZL,
    }

    interface JcStatusListener {
        fun onImuEnable(enable: Boolean)
        fun onVibratorEnable(enable: Boolean)
        fun onStartPush()
        fun onFetchNfcIr()
        fun onPlayerChange(index: Int)
    }

    private var acclPosition = 0
    private var gyroPosition = 0
    private var statusPlayer: Byte = 0
    private var statusVibrate = false
    private var statusIMU = false
    private val sharedBuffer = ByteBuffer
        .allocateDirect(REPORT_SIZE + REPORT_LARGE_SIZE + STATUS_SIZE + INPUT_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)

    private var statusListener: JcStatusListener? = null

    private fun onNativeStatusChanged(
        player: Byte,
        vibEnabled: Boolean,
        imuEnabled: Boolean,
        startPush: Boolean,
        startNfc: Boolean
    ) {
        if (player != statusPlayer) {
            statusListener?.onPlayerChange(player.toInt())
        }
        if (vibEnabled != statusVibrate) {
            statusListener?.onVibratorEnable(vibEnabled)
        }
        if (imuEnabled != statusIMU) {
            statusListener?.onImuEnable(imuEnabled)
        }
        if (startPush) {
            statusListener?.onStartPush()
        }
        if (startNfc) {
            statusListener?.onFetchNfcIr()
        }
    }

    fun setStatusChangeListener(listener: JcStatusListener?) {
        statusListener = listener
    }

    fun setInputBuffer(reportId: Int) {
        sharedBuffer.position(BUFFER_INPUT_REPORT_POSITION)
        sharedBuffer.put(reportId.toByte())
    }

    fun getInputBuffer(): Pair<Int, ByteArray> {
        val id = sharedBuffer[BUFFER_INPUT_REPORT_POSITION].toInt()
        val data = if (id == 0x31) ByteArray(REPORT_LARGE_SIZE - 1) else ByteArray(REPORT_SIZE - 1)
        sharedBuffer.position(BUFFER_INPUT_REPORT_POSITION + 1)
        sharedBuffer.get(data)
        return Pair(id, data)
    }

    fun setOutputBuffer(reportId: Int, data: ByteArray) {
        sharedBuffer.position(BUFFER_OUTPUT_REPORT_POSITION)
        sharedBuffer.put(reportId.toByte())
        sharedBuffer.put(data)
    }

    fun getOutputBuffer(): Pair<Int, ByteArray> {
        val data = ByteArray(REPORT_SIZE - 1)
        val id = sharedBuffer[BUFFER_OUTPUT_REPORT_POSITION].toInt()
        sharedBuffer.position(BUFFER_OUTPUT_REPORT_POSITION + 1)
        sharedBuffer.get(data)
        return Pair(id, data)
    }

    fun setButtons(vararg buttons: Pair<NsButton, Boolean>) {
        var button = sharedBuffer.getInt(BUFFER_BUTTON_POSITION)
        buttons.forEach {
            var v = 1
            val offset = it.first.ordinal
            v = v.shl(offset)
            button = if (it.second)
                button or v
            else
                button and v.inv()
        }
        sharedBuffer.putInt(BUFFER_BUTTON_POSITION, button)
    }

    fun setLStick(x: Int, y: Int) {
        /* uint16 */
        sharedBuffer.putChar(BUFFER_STICK_POSITION, x.toChar())
        sharedBuffer.putChar(BUFFER_STICK_POSITION + 2, y.toChar())
    }

    fun setRStick(x: Int, y: Int) {
        /* uint16 */
        sharedBuffer.putChar(BUFFER_STICK_POSITION + 4, x.toChar())
        sharedBuffer.putChar(BUFFER_STICK_POSITION + 6, y.toChar())
    }

    fun setAccelerate(x: Int, y: Int, z: Int) {
        // TODO("NOT accurate")
        val pos = BUFFER_AXES_POSITION + acclPosition * AXES_PADDING
        sharedBuffer.position(pos)
        sharedBuffer.putShort(x.toShort())
        sharedBuffer.putShort(y.toShort())
        sharedBuffer.putShort(z.toShort())
        acclPosition = (++acclPosition) % 3
        /*
        val pos1 = BUFFER_AXES_POSITION
        val pos2 = pos1 + 12
        val pos3 = pos2 + 12
        val cache1 = sharedBuffer.getShort(pos2)
        val cache2 = sharedBuffer.getShort(pos3)
        sharedBuffer.putShort(pos1, cache1)
        sharedBuffer.putShort(pos2, cache2)
        sharedBuffer.putShort(pos3, z.toShort())
    */
    }

    fun setGyroscope(x: Int, y: Int, z: Int) {
        // TODO("NOT accurate")
        val pos = BUFFER_AXES_POSITION + 6 + gyroPosition * AXES_PADDING
        sharedBuffer.position(pos)
        sharedBuffer.putShort(x.toShort())
        sharedBuffer.putShort(y.toShort())
        sharedBuffer.putShort(z.toShort())
        gyroPosition = (++gyroPosition) % 3
        /*val pos1 = BUFFER_AXES_POSITION + 6
        val pos2 = pos1 + 12
        val pos3 = pos2 + 12
        sharedBuffer.putShort(pos, x.toShort())
        sharedBuffer.putShort(pos, y.toShort())
        sharedBuffer.putShort(pos, z.toShort())*/
    }

    fun init(assetManager: AssetManager, path: String) {
        nativeInit(sharedBuffer, assetManager, path)
        val half = STICK_MAX_VALUE / 2
        setLStick(half, half)
        setRStick(half, half)
    }

    external fun nativeInit(buffer: ByteBuffer, assetManager: AssetManager, path: String): Int

    external fun nativeFree(): Int

    external fun nativeReplayOutputReport(): Int

    external fun nativeMakeupInputReport(): Int

    external fun nativeUpdateButtons(buttons: BooleanArray/*1bit*/)

    external fun nativeUpdateSticks(sticks: CharArray/*16bit*/)

    external fun nativeUpdateIMU(accl: ShortArray?/*16bit*/, gyro: ShortArray?)

    external fun nativeUpdateNFC(nfc: ByteArray)

    external fun nativeUpdateIR(ir: ByteArray)

    companion object {
        const val STICK_MAX_VALUE = 4096
        const val IMU_MAX_SIZE = 9
        const val REPORT_SIZE = 49
        const val REPORT_LARGE_SIZE = 362
        const val STATUS_SIZE = 12
        const val INPUT_SIZE = 176
        const val AXES_PADDING = 12
        const val BUFFER_OUTPUT_REPORT_POSITION = 0
        const val BUFFER_INPUT_REPORT_POSITION = BUFFER_OUTPUT_REPORT_POSITION + REPORT_SIZE
        const val BUFFER_INPUT_POSITION = BUFFER_INPUT_REPORT_POSITION + REPORT_LARGE_SIZE
        const val BUFFER_STATUS_POSITION = BUFFER_INPUT_POSITION + INPUT_SIZE
        const val BUFFER_BUTTON_POSITION = BUFFER_INPUT_POSITION
        const val BUFFER_STICK_POSITION = BUFFER_BUTTON_POSITION + 4
        const val BUFFER_AXES_POSITION = BUFFER_STICK_POSITION + 8
        const val BUFFER_NFC_POSITION = BUFFER_AXES_POSITION + 36

        init {
            System.loadLibrary("joycon")
        }
    }
}
