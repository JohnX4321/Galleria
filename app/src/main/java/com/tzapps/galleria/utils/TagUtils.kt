package com.tzapps.galleria.utils

import android.content.Context
import android.location.Geocoder
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.round

object TagUtils {

    fun getDimensions(exifInterface: ExifInterface): String {
        try {
            val height = ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_IMAGE_LENGTH)
            val width=ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_IMAGE_WIDTH)
            return "$width x $height"
        } catch (e: Exception){
            e.printStackTrace()
        }
        return ExifUtils.NO_DATA
    }

    fun getLocation(exifInterface: ExifInterface): String {
        val latValue = ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_GPS_LATITUDE)
        val longValue = ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_GPS_LONGITUDE)
        if (latValue!= null && longValue!= null) {
            val posLat =
                ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_GPS_LATITUDE_REF).toString() == "N"
            val lat = parseGPSLongOrLat(latValue.toString(),posLat).toDouble()
            val posLong=ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_GPS_LONGITUDE_REF).toString() == "E"
            val lon= parseGPSLongOrLat(longValue.toString(),posLong)
            return "$lat,$lon"
        }
        return ExifUtils.NO_DATA
    }

    fun getExposure(exifInterface: ExifInterface): String {
        val expValue = ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_EXPOSURE_TIME)
        return if (expValue!=null) parseExposureTime(expValue.toString()) else ExifUtils.NO_DATA
    }

    fun getFocalLength(exifInterface: ExifInterface): String {
        var fl = ExifUtils.NO_DATA
        try {
            val flValue = ExifUtils.getCastValue(exifInterface, ExifInterface.TAG_FOCAL_LENGTH)
            if (flValue != null) {
                fl = flValue.toString()
                val z = fl.split("/")
                val a = z[0].toInt();
                val b = z[1].toInt()
                fl="${a.toFloat()/b.toFloat()} mm"
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
        return fl
    }

    fun getMakeAndModel(exifInterface: ExifInterface): String {
        val makeValue = ExifUtils.getCastValue(exifInterface, ExifInterface.TAG_MAKE)
        val modelValue = ExifUtils.getCastValue(exifInterface, ExifInterface.TAG_MODEL)
        return if (makeValue != null && modelValue != null) "$makeValue $modelValue" else ExifUtils.NO_DATA
    }

    fun getAperture(exifInterface: ExifInterface): String {
        val apertureValue = ExifUtils.getCastValue(exifInterface, ExifInterface.TAG_F_NUMBER)
        return if (apertureValue != null) "ƒ/$apertureValue" else ExifUtils.NO_DATA
    }

    fun getISO(exifInterface: ExifInterface): String {
        val isoValue = ExifUtils.getCastValue(exifInterface,ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
        return isoValue?.toString() ?: ExifUtils.NO_DATA
    }

    suspend fun getVideoFrameRate(path: String): String {
        val fr = retrieveFrameRate(path)
        return if (fr!=-1) "$fr fps" else ExifUtils.NO_DATA
    }

    private suspend fun retrieveFrameRate(path: String): Int {
        var fr=-1
        withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(path)
                val numTracks = extractor.trackCount
                for (i in 0..numTracks) {
                    val format = extractor.getTrackFormat(i)
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
                        fr=format.getInteger(MediaFormat.KEY_FRAME_RATE)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }finally {
                extractor.release()
            }
        }
        return fr
    }

    fun getAddress(context: Context,exifInterface: ExifInterface): String {
        val latLon = getLocation(exifInterface)
        if (latLon==ExifUtils.NO_DATA) return latLon
        val a=latLon.split(",")
        val lat=a[0].toDouble();val lon=a[1].toDouble()
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addr = geocoder.getFromLocation(lat,lon,1)
            if (addr.isNotEmpty()) return addr[0].toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ExifUtils.NO_DATA
    }

    private fun parseExposureTime(input: String?): String {
        if (input==null||input=="null")
            return ExifUtils.NO_DATA
        val f=input.toFloat()
        return try {
            val i= round(1/f)
            "1/$i sec"
        } catch (e: NumberFormatException){
            input
        }
    }

    private fun parseGPSLongOrLat(input: String?,positive: Boolean): String {
        if (input==null||input=="null") return ExifUtils.NO_DATA
        var value=0F
        val parts=input.split(",")
        for (i in parts.indices) {
            val p=parts[i].split("/")
            val a=p[0].toInt();var b=p[1].toInt()
            var factor=1
            for (k in 0..i) factor*=60
            b*=factor
            value+=a.toFloat()/b.toFloat()
        }
        if (!positive) value*=-1f
        return value.toString()
    }

}