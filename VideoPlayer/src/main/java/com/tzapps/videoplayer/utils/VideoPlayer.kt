package com.tzapps.videoplayer.utils

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.text.TextUtils
import android.view.GestureDetector
import android.view.MotionEvent
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.MergingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.text.Subtitle
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.tzapps.videoplayer.db.SingleVideo
import com.tzapps.videoplayer.db.VideoSource

class VideoPlayer(val context: Context, val playerView: PlayerView, val videoSource: SingleVideo, val playerController: PlayerController) {

    companion object {
        val CLASS_NAME = VideoPlayer::class.simpleName
    }

    private var exoPlayer: ExoPlayer? = null
    private var mediaSource: MediaSource
    private var trackSelector: DefaultTrackSelector
    private var componentListener: ComponentListener
    private var cacheDataSourceFactory: CacheDataSourceFactory
    var isLock = false
    private var width = 0
    private var index = 0

    init {
        playerView.requestFocus()
        componentListener=ComponentListener()
        cacheDataSourceFactory= CacheDataSourceFactory(context,1024*1024*1024,5*1024*1024)
        trackSelector= DefaultTrackSelector(context)
        exoPlayer=ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
        playerView.player=exoPlayer
        playerView.keepScreenOn=true
        exoPlayer!!.playWhenReady=true
        exoPlayer!!.addListener(componentListener)
        mediaSource=buildMediaSource(videoSource.uri!!,cacheDataSourceFactory)
        exoPlayer!!.setMediaSource(mediaSource)
        exoPlayer!!.prepare()
        seekToSelectedPosition(videoSource.watchedLength,false)
        /*if (videoSource.videos!!.size==1||isLastVideo())
            playerController.disableNextButtonOnLastVideo(true)*/
    }

    fun pausePlayer() { exoPlayer?.playWhenReady=false }

    fun resumePlayer() { exoPlayer?.playWhenReady=true }

    fun releasePlayer(){
        if (exoPlayer==null) return
        playerController.setVideoWatchedLength()
        playerView.player=null
        exoPlayer?.release()
        exoPlayer?.removeListener(componentListener)
        exoPlayer=null
    }

    fun getPlayer() = exoPlayer

    fun getCurrentVideo() = videoSource.uri

    fun buildMediaSource(uri: Uri,cacheDataSourceFactory: CacheDataSourceFactory): MediaSource {
        @C.ContentType val type = Util.inferContentType(uri)
        return when(type) {
            C.TYPE_SS-> SsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            C.TYPE_DASH-> DashMediaSource.Factory(cacheDataSourceFactory).createMediaSource(
                MediaItem.fromUri(uri))
            C.TYPE_HLS-> HlsMediaSource.Factory(cacheDataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
            else-> ProgressiveMediaSource.Factory(cacheDataSourceFactory).createMediaSource(
                MediaItem.fromUri(uri))
        }
    }

    fun setMute(mute: Boolean){
        val currVolume = exoPlayer!!.volume
        if (currVolume>0&&mute){
            exoPlayer!!.volume=0f
            playerController.setMuteMode(true)
        } else if (!mute&&currVolume==0f){
            exoPlayer!!.volume=1f
            playerController.setMuteMode(false)
        }
    }

    fun seekToSelectedPosition(hour: Int, minute: Int, second: Int) {
        exoPlayer!!.seekTo(((hour*3600+minute*60+second)*1000).toLong())
    }

    fun seekToSelectedPosition(ms: Long, rewind: Boolean){
        if (rewind){
            exoPlayer!!.seekTo(exoPlayer!!.currentPosition-15000)
            return
        }
        exoPlayer!!.seekTo(ms*1000)
    }

    fun seekToDoubleTap(){
        getScreenWidth()
        val gestureDetector = GestureDetector(context,object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent?): Boolean {
                val posOfDoubleTapX = e!!.x
                if (posOfDoubleTapX<width/2)
                    exoPlayer?.seekTo(exoPlayer?.currentPosition!!-5000)
                else
                    exoPlayer?.seekTo(exoPlayer?.currentPosition!!+5000)
                return true
            }
        })
        playerView.setOnTouchListener { _, event -> gestureDetector.onTouchEvent(event) }
    }

    private fun getScreenWidth() {
        width=Resources.getSystem().displayMetrics.widthPixels
    }

    fun seekToNext(){
        /*if (index<videoSource.videos!!.lastIndex) {
            setCurrentVideoPosition()
            index++
            mediaSource=buildMediaSource(videoSource.videos!![index]!!,cacheDataSourceFactory)
            exoPlayer!!.setMediaSource(mediaSource)
            exoPlayer!!.prepare()
            if (videoSource.videos!![index]!!.watchedLength!=-1L)
                seekToSelectedPosition(videoSource.videos!![index]!!.watchedLength,false)
            if (isLastVideo())
                playerController.disableNextButtonOnLastVideo(true)
        }*/
    }

    //private fun isLastVideo() = index==videoSource.videos?.lastIndex

    fun seekToPrevious() {
        playerController.disableNextButtonOnLastVideo(false)
        if (index==0) {
            seekToSelectedPosition(0,false)
            return
        }
        /*if (index>0) {
            setCurrentVideoPosition()
            index--
            mediaSource=buildMediaSource(videoSource.videos!![index]!!,cacheDataSourceFactory)
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            if (videoSource.videos!![index]!!.watchedLength!=-1L)
                seekToSelectedPosition(videoSource.videos!![index]!!.watchedLength,false)
        }*/
    }

    private fun setCurrentVideoPosition(): Long {
        if (getCurrentVideo()==null)
            return 0L
        return exoPlayer!!.currentPosition/1000
    }

    //subtitles
    /*fun setSelectedSubtitle(subtitle: Subtitle) {
        if (TextUtils.isEmpty(subtitle.title))
            return
        val subtitleFormat = Format.createTextFormat(null,MimeTypes.APPLICATION_SUBRIP,Format.NO_VALUE,null)
        val subtitleSource = SingleSampleMediaSource.Factory(cacheDataSourceFactory)
            .createMediaSource(Uri.parse(subtitle.subtitleUrl),subtitleFormat,C.TIME_UNSET)
        playerController.changeSubtitleBackground()
        exoPlayer?.setMediaSource(MergingMediaSource(mediaSource,subtitleSource))
        exoPlayer?.prepare()
        playerController.showSubtitle(true)
        resumePlayer()
    }*/






    private inner class ComponentListener: Player.Listener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when(playbackState) {
                Player.STATE_IDLE->
                    playerController.showProgressBar(false)
                Player.STATE_BUFFERING->
                    playerController.showProgressBar(true)
                Player.STATE_READY->{
                    playerController.showProgressBar(false)
                    playerController.audioFocus()
                }
                Player.STATE_ENDED->{
                    playerController.showProgressBar(false)
                    playerController.videoEnded()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            playerController.showProgressBar(false)
        }

    }

}