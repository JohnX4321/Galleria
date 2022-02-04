package com.tzapps.videoplayer.utils

interface PlayerController {

    fun setMuteMode(mute: Boolean)
    fun showProgressBar(visible: Boolean)
    fun showSubtitle(show: Boolean)
    fun changeSubtitleBackground()
    fun audioFocus()
    fun setVideoWatchedLength()
    fun videoEnded()
    fun disableNextButtonOnLastVideo(disable: Boolean)

}