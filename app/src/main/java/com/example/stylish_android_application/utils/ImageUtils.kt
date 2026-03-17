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

    // Processes the profile image: rotates it correctly based on EXIF and compresses it
    fun processProfileImage(context: Context, uri: Uri): Pair<Bitmap, ByteArray>? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 1. Scale down to 1080p
            val maxDimension = 1080
            var bitmap = originalBitmap
            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                val ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
                val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
                val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            // 2. Fix rotation using EXIF data
            var rotatedBitmap = bitmap
            val exifInputStream = context.contentResolver.openInputStream(uri)
            if (exifInputStream != null) {
                val exif = androidx.exifinterface.media.ExifInterface(exifInputStream)
                val orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = android.graphics.Matrix()
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                if (orientation != androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL &&
                    orientation != androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED) {
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                exifInputStream.close()
            }

            // 3. Compress to ByteArray for Firebase Storage
            val baos = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)

            Pair(rotatedBitmap, baos.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}