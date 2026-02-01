package com.example.y50_printer

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * Common utility functions
 */
object CommonUtil {
    
    private var context: Context? = null
    
    fun init(ctx: Context) {
        context = ctx.applicationContext
    }
    
    /**
     * Convert byte array to hex string
     */
    fun byte2Hex(bytes: ByteArray?): String {
        if (bytes == null) return ""
        val hex = StringBuilder()
        for (b in bytes) {
            hex.append(String.format("%02X ", b))
        }
        return hex.toString().trim()
    }
    
    /**
     * Compare two objects for equality
     */
    fun equals(obj1: Any?, obj2: Any?): Boolean {
        return obj1 == obj2
    }
    
    /**
     * Get bitmap from resource ID
     */
    fun getBitmap(resId: Int): Bitmap {
        val ctx = context
            ?: throw IllegalStateException("CommonUtil not initialized. Call CommonUtil.init(context) first")
        return BitmapFactory.decodeResource(ctx.resources, resId)
    }
    
    /**
     * Zoom/resize image
     */
    fun zoomImg(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}
