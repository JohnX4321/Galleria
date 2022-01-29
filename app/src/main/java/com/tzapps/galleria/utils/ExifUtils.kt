package com.tzapps.galleria.utils


import android.content.Context
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import java.io.InputStream
import java.util.*
import java.util.concurrent.Executors

/**
 * Forked from Camera Roll App
 * as it serves the purpose
 * https://github.com/kollerlukas/Camera-Roll-Android-App/blob/master/app/src/main/java/us/koller/cameraroll/util/ExifUtil.java
 **/
object ExifUtils {

    const val NO_DATA = "Unknown"

    private const val TYPE_UNDEFINED = -1
    private const val TYPE_STRING = 0
    private const val TYPE_INT = 1
    private const val TYPE_DOUBLE = 2
    private const val TYPE_RATIONAL = 3

    //Tags
    private val exifTags = arrayOf<String>(
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_BITS_PER_SAMPLE,
        ExifInterface.TAG_BRIGHTNESS_VALUE,
        ExifInterface.TAG_CFA_PATTERN,
        ExifInterface.TAG_COLOR_SPACE,
        ExifInterface.TAG_COMPONENTS_CONFIGURATION,
        ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,
        ExifInterface.TAG_COMPRESSION,
        ExifInterface.TAG_CONTRAST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_CUSTOM_RENDERED,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DEFAULT_CROP_SIZE,
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,
        ExifInterface.TAG_DNG_VERSION,
        ExifInterface.TAG_EXIF_VERSION,
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_EXPOSURE_INDEX,
        ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_FILE_SOURCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FLASHPIX_VERSION,
        ExifInterface.TAG_FLASH_ENERGY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_GAIN_CONTROL,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_IMAGE_DESCRIPTION,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_IMAGE_UNIQUE_ID,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_INTEROPERABILITY_INDEX,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,
        ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_NEW_SUBFILE_TYPE,
        ExifInterface.TAG_OECF,
        ExifInterface.TAG_ORF_ASPECT_FRAME,
        ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
        ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
        ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,
        ExifInterface.TAG_PIXEL_X_DIMENSION,
        ExifInterface.TAG_PIXEL_Y_DIMENSION,
        ExifInterface.TAG_PLANAR_CONFIGURATION,
        ExifInterface.TAG_PRIMARY_CHROMATICITIES,
        ExifInterface.TAG_REFERENCE_BLACK_WHITE,
        ExifInterface.TAG_RELATED_SOUND_FILE,
        ExifInterface.TAG_RESOLUTION_UNIT,
        ExifInterface.TAG_ROWS_PER_STRIP,
        ExifInterface.TAG_RW2_ISO,
        ExifInterface.TAG_RW2_JPG_FROM_RAW,
        ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
        ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
        ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
        ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
        ExifInterface.TAG_SAMPLES_PER_PIXEL,
        ExifInterface.TAG_SATURATION,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        ExifInterface.TAG_SCENE_TYPE,
        ExifInterface.TAG_SENSING_METHOD,
        ExifInterface.TAG_SHARPNESS,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
        ExifInterface.TAG_STRIP_BYTE_COUNTS,
        ExifInterface.TAG_STRIP_OFFSETS,
        ExifInterface.TAG_SUBFILE_TYPE,
        ExifInterface.TAG_SUBJECT_AREA,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_SUBJECT_LOCATION,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
        ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
        ExifInterface.TAG_TRANSFER_FUNCTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_WHITE_POINT,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
        ExifInterface.TAG_Y_CB_CR_POSITIONING,
        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
        ExifInterface.TAG_Y_RESOLUTION
    )

    //Types
    private val exifTypes = intArrayOf(
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_INT,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_DOUBLE,
        TYPE_INT,
        TYPE_STRING,
        TYPE_DOUBLE,
        TYPE_RATIONAL,
        TYPE_INT,
        TYPE_INT,
        TYPE_DOUBLE,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_INT,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_DOUBLE,
        TYPE_INT,
        TYPE_UNDEFINED,
        TYPE_UNDEFINED,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_RATIONAL,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_UNDEFINED,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_UNDEFINED,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_DOUBLE,
        TYPE_INT,
        TYPE_INT,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_STRING,
        TYPE_INT,
        TYPE_INT,
        TYPE_INT,
        TYPE_STRING,
        TYPE_INT,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_RATIONAL,
        TYPE_INT,
        TYPE_INT,
        TYPE_RATIONAL
    )

    //Values
    private val exifValues = arrayOf(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null, arrayOf(
            "ORIENTATION_UNDEFINED (= 0)",
            "ORIENTATION_NORMAL (= 1)",
            "ORIENTATION_FLIP_HORIZONTAL (= 2)",
            "ORIENTATION_ROTATE_180 (= 3)",
            "ORIENTATION_FLIP_VERTICAL (= 4)",
            "ORIENTATION_TRANSPOSE (= 5)",
            "ORIENTATION_ROTATE_90 (= 6)",
            "ORIENTATION_TRANSVERSE (= 7)",
            "ORIENTATION_ROTATE_270 (= 8)"
        ),
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null, arrayOf(
            "WHITEBALANCE_AUTO (= 0)",
            "WHITEBALANCE_MANUAL (= 1)"
        ),
        null,
        null,
        null,
        null,
        null,
        null
    )

    // The Values that are removed when the Exif data is cleared
    private val valuesToRemove = arrayOf<String>(
        ExifInterface.TAG_APERTURE_VALUE,
        ExifInterface.TAG_ARTIST,  /*ExifInterface.TAG_BITS_PER_SAMPLE,*/ /*ExifInterface.TAG_BRIGHTNESS_VALUE,*/ /*ExifInterface.TAG_CFA_PATTERN,*/ /*ExifInterface.TAG_COLOR_SPACE,*/ /*ExifInterface.TAG_COMPONENTS_CONFIGURATION,*/ /*ExifInterface.TAG_COMPRESSED_BITS_PER_PIXEL,*/ /*ExifInterface.TAG_COMPRESSION,*/ /*ExifInterface.TAG_CONTRAST,*/
        ExifInterface.TAG_COPYRIGHT,  /*ExifInterface.TAG_CUSTOM_RENDERED,*/
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_DATETIME_ORIGINAL,  /*ExifInterface.TAG_DEFAULT_CROP_SIZE,*/
        ExifInterface.TAG_DEVICE_SETTING_DESCRIPTION,
        ExifInterface.TAG_DIGITAL_ZOOM_RATIO,  /*ExifInterface.TAG_DNG_VERSION,*/ /*ExifInterface.TAG_EXIF_VERSION,*/
        ExifInterface.TAG_EXPOSURE_BIAS_VALUE,
        ExifInterface.TAG_EXPOSURE_INDEX,
        ExifInterface.TAG_EXPOSURE_MODE,
        ExifInterface.TAG_EXPOSURE_PROGRAM,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_FILE_SOURCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_FLASHPIX_VERSION,
        ExifInterface.TAG_FLASH_ENERGY,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM,
        ExifInterface.TAG_FOCAL_PLANE_RESOLUTION_UNIT,
        ExifInterface.TAG_FOCAL_PLANE_X_RESOLUTION,
        ExifInterface.TAG_FOCAL_PLANE_Y_RESOLUTION,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_GAIN_CONTROL,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_DEST_BEARING,
        ExifInterface.TAG_GPS_DEST_BEARING_REF,
        ExifInterface.TAG_GPS_DEST_DISTANCE,
        ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
        ExifInterface.TAG_GPS_DEST_LATITUDE,
        ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
        ExifInterface.TAG_GPS_DEST_LONGITUDE,
        ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
        ExifInterface.TAG_GPS_DIFFERENTIAL,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_MEASURE_MODE,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SATELLITES,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_STATUS,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_VERSION_ID,
        ExifInterface.TAG_IMAGE_DESCRIPTION,  /*ExifInterface.TAG_IMAGE_LENGTH,*/
        ExifInterface.TAG_IMAGE_UNIQUE_ID,  /*ExifInterface.TAG_IMAGE_WIDTH,*/
        ExifInterface.TAG_INTEROPERABILITY_INDEX,
        ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,  /*ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT,*/ /*ExifInterface.TAG_JPEG_INTERCHANGE_FORMAT_LENGTH,*/
        ExifInterface.TAG_LIGHT_SOURCE,
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MAKER_NOTE,
        ExifInterface.TAG_MAX_APERTURE_VALUE,
        ExifInterface.TAG_METERING_MODE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_NEW_SUBFILE_TYPE,
        ExifInterface.TAG_OECF,
        ExifInterface.TAG_ORF_ASPECT_FRAME,
        ExifInterface.TAG_ORF_PREVIEW_IMAGE_LENGTH,
        ExifInterface.TAG_ORF_PREVIEW_IMAGE_START,
        ExifInterface.TAG_ORF_THUMBNAIL_IMAGE,  /*ExifInterface.TAG_ORIENTATION,*/
        ExifInterface.TAG_PHOTOMETRIC_INTERPRETATION,  /*ExifInterface.TAG_PIXEL_X_DIMENSION,*/ /*ExifInterface.TAG_PIXEL_Y_DIMENSION,*/
        ExifInterface.TAG_PLANAR_CONFIGURATION,
        ExifInterface.TAG_PRIMARY_CHROMATICITIES,
        ExifInterface.TAG_REFERENCE_BLACK_WHITE,
        ExifInterface.TAG_RELATED_SOUND_FILE,
        ExifInterface.TAG_RESOLUTION_UNIT,
        ExifInterface.TAG_ROWS_PER_STRIP,
        ExifInterface.TAG_RW2_ISO,
        ExifInterface.TAG_RW2_JPG_FROM_RAW,
        ExifInterface.TAG_RW2_SENSOR_BOTTOM_BORDER,
        ExifInterface.TAG_RW2_SENSOR_LEFT_BORDER,
        ExifInterface.TAG_RW2_SENSOR_RIGHT_BORDER,
        ExifInterface.TAG_RW2_SENSOR_TOP_BORDER,
        ExifInterface.TAG_SAMPLES_PER_PIXEL,
        ExifInterface.TAG_SATURATION,
        ExifInterface.TAG_SCENE_CAPTURE_TYPE,
        ExifInterface.TAG_SCENE_TYPE,
        ExifInterface.TAG_SENSING_METHOD,
        ExifInterface.TAG_SHARPNESS,
        ExifInterface.TAG_SHUTTER_SPEED_VALUE,
        ExifInterface.TAG_SOFTWARE,
        ExifInterface.TAG_SPATIAL_FREQUENCY_RESPONSE,
        ExifInterface.TAG_SPECTRAL_SENSITIVITY,
        ExifInterface.TAG_STRIP_BYTE_COUNTS,
        ExifInterface.TAG_STRIP_OFFSETS,
        ExifInterface.TAG_SUBFILE_TYPE,
        ExifInterface.TAG_SUBJECT_AREA,
        ExifInterface.TAG_SUBJECT_DISTANCE,
        ExifInterface.TAG_SUBJECT_DISTANCE_RANGE,
        ExifInterface.TAG_SUBJECT_LOCATION,
        ExifInterface.TAG_SUBSEC_TIME,
        ExifInterface.TAG_SUBSEC_TIME_DIGITIZED,
        ExifInterface.TAG_SUBSEC_TIME_ORIGINAL,
        ExifInterface.TAG_THUMBNAIL_IMAGE_LENGTH,
        ExifInterface.TAG_THUMBNAIL_IMAGE_WIDTH,
        ExifInterface.TAG_TRANSFER_FUNCTION,
        ExifInterface.TAG_USER_COMMENT,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_WHITE_POINT,
        ExifInterface.TAG_X_RESOLUTION,
        ExifInterface.TAG_Y_CB_CR_COEFFICIENTS,
        ExifInterface.TAG_Y_CB_CR_POSITIONING,
        ExifInterface.TAG_Y_CB_CR_SUB_SAMPLING,
        ExifInterface.TAG_Y_RESOLUTION
    )

    interface Callback {
        fun done(success: Boolean)
    }

    fun getExifInterface(context: Context, uri: Uri?): ExifInterface? {
        if (uri == null) {
            return null
        }
        var exif: ExifInterface? = null
        try {
            val `is`: InputStream? = context.contentResolver.openInputStream(uri)
            if (`is` != null) {
                exif = ExifInterface(`is`)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } catch (e: SecurityException) {
            e.printStackTrace()
            return null
        }
        return exif
    }

    private fun getTypeForTag(tag: String): Int {
        val tags: List<String> = getExifTags().toList()
        val index = tags.indexOf(tag)
        return getExifTypes()[index]
    }

    fun getExifTags(): Array<String> {
        return exifTags
    }

    private fun getExifTypes(): IntArray {
        return exifTypes
    }

    fun getExifValues(): Array<Array<String>?>? {
        return exifValues
    }

    @Throws(NumberFormatException::class, NullPointerException::class)
    fun getCastValue(exif: ExifInterface, tag: String): Any? {
        val value: String = exif.getAttribute(tag)?:""
        return castValue(tag, value)
    }

    @Throws(NumberFormatException::class, NullPointerException::class)
    private fun castValue(tag: String, value: String?): Any? {
        if (value == null || value == "") {
            return null
        }
        val type: Int = getTypeForTag(tag)
        var castValue: Any? = null
        when (type) {
            TYPE_UNDEFINED, TYPE_STRING ->                 //do nothing
                castValue = value
            TYPE_INT -> castValue = Integer.valueOf(value)
            TYPE_DOUBLE -> castValue = java.lang.Double.valueOf(value)
            TYPE_RATIONAL -> castValue = value
            else -> {
            }
        }
        return castValue ?: value
    }

    fun retrieveExifData(context: Context, uri: Uri?): Array<ExifItem?>? {
        val exif: ExifInterface? = getExifInterface(context, uri)
        if (exif != null) {
            val exifTags = getExifTags()
            val exifData = arrayOfNulls<ExifItem>(exifTags.size)
            for (i in exifTags.indices) {
                val tag = exifTags[i]
                val value: String = exif.getAttribute(tag)?:""
                val exifItem = ExifItem(tag, value)
                exifData[i] = exifItem
            }
            return exifData
        }
        return null
    }

    fun saveExifData(path: String, exifData: Array<ExifItem>) {
        try {
            val exif = ExifInterface(path)
            for (i in exifData.indices) {
                val exifItem = exifData[i]
                exif.setAttribute(exifItem.tag!!, exifItem.value)
            }
            exif.saveAttributes()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }

    fun getExifOrientationAngle(context: Context, uri: Uri?): Int {
        val exif: ExifInterface = getExifInterface(context, uri) ?: return 0
        val orientation = getCastValue(exif, ExifInterface.TAG_ORIENTATION) as Int
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    fun saveChanges(exifInterface: ExifInterface?, editedItems: List<ExifItem>) {
        saveChanges(exifInterface, editedItems, null)
    }

    fun saveChanges(
        exifInterface: ExifInterface?,
        editedItems: List<ExifItem>, callback: Callback?
    ) {
        if (exifInterface == null) {
            return
        }
        Executors.newSingleThreadExecutor().execute {
            var success = true
            for (item in editedItems) {
                exifInterface.setAttribute(item.tag?:"", item.value)
            }
            try {
                exifInterface.saveAttributes()
            } catch (e: IOException) {
                e.printStackTrace()
                success = false
            }
            callback?.done(success)
        }
    }

    fun removeExifData(exifInterface: ExifInterface?) {
        removeExifData(exifInterface, null)
    }

    fun removeExifData(exifInterface: ExifInterface?, callback: Callback?) {
        if (exifInterface == null) {
            return
        }
        Executors.newSingleThreadExecutor().execute {
            var success = true
            // remove all Exif data
            for (tag in valuesToRemove) {
                exifInterface.setAttribute(tag, null)
            }
            try {
                exifInterface.saveAttributes()
            } catch (e: IOException) {
                e.printStackTrace()
                success = false
            }
            callback?.done(success)
        }
    }

    class ExifItem(val tag: String?, var value: String?) : Parcelable {

        override fun toString(): String {
            return "(Tag: $tag, Value: $value)"
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeString(tag)
            parcel.writeString(value)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<ExifItem> = object : Parcelable.Creator<ExifItem> {
                override fun createFromParcel(`in`: Parcel): ExifItem {
                    val tag = `in`.readString()
                    val value = `in`.readString()
                    return ExifItem(tag, value)
                }

                override fun newArray(size: Int): Array<ExifItem?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

}