package com.tzapps.videoplayer.db

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoSource(var videos: List<SingleVideo?>? = null,var selectedSrcIndex: Int): Parcelable {



}

@Parcelize
data class SingleVideo(var uri: Uri? = null, var watchedLength: Long = -1L): Parcelable {

}
