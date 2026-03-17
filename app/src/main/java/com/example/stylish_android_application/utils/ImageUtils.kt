package com.example.stylish_android_application.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {

    // הפונקציה מקבלת Uri מהגלריה, ומחזירה גם את התמונה (בשביל הג'מיני) וגם את הבתים המכווצים (בשביל פיירבייס)
    fun processImageForUpload(context: Context, uri: Uri): Pair<Bitmap, ByteArray>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            var bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // 1. הקטנת רזולוציה ל-1080p
            val maxResolution = 1080
            if (bitmap.width > maxResolution || bitmap.height > maxResolution) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth = if (ratio > 1) maxResolution else (maxResolution * ratio).toInt()
                val newHeight = if (ratio > 1) (maxResolution / ratio).toInt() else maxResolution
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            // 2. דחיסת משקל (75% איכות)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)

            // מחזירים צמד: התמונה המוכנה, והבתים להעלאה
            Pair(bitmap, baos.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}