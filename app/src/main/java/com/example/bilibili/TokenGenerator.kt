package com.example.bilibili

import android.util.Base64
import java.nio.ByteBuffer

object TokenGenerator {
    fun generateToken(
        projectId: Int,
        screenId: Int,
        skuId: Int,
        count: Int,
        orderType: Int,
        timestampSec: Long
    ): String {
        val buffer = ByteBuffer.allocate(20)
        buffer.put(0xC0.toByte())
        buffer.putInt(timestampSec.toInt())
        buffer.putInt(projectId)
        buffer.putInt(screenId)
        buffer.put(orderType.toByte())
        buffer.putShort(count.toShort())
        buffer.putInt(skuId)

        val array = buffer.array()
        val b64Encoded = Base64.encodeToString(array, Base64.NO_WRAP)
        
        return b64Encoded
            .replace('/', '_')
            .replace('+', '-')
            .replace('=', '.')
    }

    fun generateCtoken(
        touchend: Int = (20..50).random(),
        scrollX: Int = 0,
        visibilitychange: Int = (10..45).random(),
        scrollY: Int = 0,
        innerWidth: Int = 255,
        openWindow: Int = (10..45).random(),
        innerHeight: Int = 255,
        outerWidth: Int = 255,
        timer: Int = (1..8).random(),
        ticketCollectionT: Int = 0,
        outerHeight: Int = 255,
        screenX: Int = 0,
        screenY: Int = 0,
        screenWidth: Int = 255
    ): String {
        val list = mutableListOf<Byte>()
        
        fun addByteField(value: Int) {
            list.add(value.toByte())
            list.add(0x00.toByte())
        }
        
        addByteField(touchend)
        addByteField(scrollX)
        addByteField(visibilitychange)
        addByteField(scrollY)
        addByteField(innerWidth)
        addByteField(openWindow)
        addByteField(innerHeight)
        addByteField(outerWidth)
        
        val timerBytes = ByteBuffer.allocate(2).putShort(timer.toShort()).array()
        list.add(timerBytes[0])
        list.add(0x00.toByte())
        list.add(timerBytes[1])
        list.add(0x00.toByte())
        
        val tkBytes = ByteBuffer.allocate(2).putShort(ticketCollectionT.toShort()).array()
        list.add(tkBytes[0])
        list.add(0x00.toByte())
        list.add(tkBytes[1])
        list.add(0x00.toByte())
        
        addByteField(outerHeight)
        addByteField(screenX)
        addByteField(screenY)
        addByteField(screenWidth)
        
        return Base64.encodeToString(list.toByteArray(), Base64.NO_WRAP)
    }
}
