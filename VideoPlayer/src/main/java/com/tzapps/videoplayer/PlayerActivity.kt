package com.tzapps.videoplayer

import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.ui.CaptionStyleCompat
import com.google.android.exoplayer2.ui.PlayerView
import com.tzapps.videoplayer.db.SingleVideo
import com.tzapps.videoplayer.db.VideoSource
import com.tzapps.videoplayer.utils.PlayerController
import com.tzapps.videoplayer.utils.VideoPlayer

class PlayerActivity: AppCompatActivity(),View.OnClickListener,PlayerController {

    private lateinit var playerView: PlayerView
    private var player: VideoPlayer? = null
    private lateinit var alertDialog: AlertDialog
    private var videoSource: SingleVideo? = null
    private var audioManager: AudioManager? = null
    private var disableBackPress = false
    private lateinit var progressBar: ProgressBar
    private lateinit var muteBtn: ImageButton
    private lateinit var unmuteBtn: ImageButton
    private lateinit var settingBtn: ImageButton
    private lateinit var lockBtn: ImageButton
    private lateinit var unlockBtn: ImageButton
    private lateinit var nextBtn: ImageButton
    private lateinit var prevBtn: ImageButton
    private lateinit var retryBtn: ImageButton
    private lateinit var backBtn: ImageButton
    private lateinit var uri: Uri

    private val onAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when(focusChange) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,AudioManager.AUDIOFOCUS_LOSS->{
                if (player!=null)
                    player!!.getPlayer()?.playWhenReady=false
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        videoSource=intent!!.getParcelableExtra("videoSource")
        playerView=findViewById(R.id.demo_player_view)
        progressBar=findViewById(R.id.progress_bar)
        muteBtn=findViewById(R.id.btn_mute)
        unmuteBtn=findViewById(R.id.btn_unMute)
        lockBtn=findViewById(R.id.btn_lock)
        unlockBtn=findViewById(R.id.btn_unLock)
        nextBtn=findViewById(R.id.btn_next)
        prevBtn=findViewById(R.id.btn_prev)
        retryBtn=findViewById(R.id.retry_btn)
        backBtn=findViewById(R.id.btn_back)
        playerView.subtitleView!!.visibility=View.GONE
        muteBtn.setOnClickListener(this)
        unmuteBtn.setOnClickListener(this)
        settingBtn.setOnClickListener(this)
        lockBtn.setOnClickListener(this)
        unlockBtn.setOnClickListener(this)
        nextBtn.setOnClickListener(this)
        prevBtn.setOnClickListener(this)
        retryBtn.setOnClickListener(this)
        backBtn.setOnClickListener(this)
        if (videoSource?.uri==null)
            return
        player= VideoPlayer(applicationContext,playerView,videoSource!!,this)
        audioManager=applicationContext.getSystemService(AudioManager::class.java)
        player!!.seekToDoubleTap()
        playerView.setControllerVisibilityListener { v->
            if (player!!.isLock)
                playerView.hideController()
            backBtn.visibility = if (v==View.VISIBLE && !player!!.isLock) View.VISIBLE else View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        player?.resumePlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        player?.resumePlayer()
    }

    override fun onPause() {
        super.onPause()
        player?.releasePlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (audioManager!=null) {
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
                audioManager!!.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange->
                        when(focusChange) {
                            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,AudioManager.AUDIOFOCUS_LOSS->{
                                if (player!=null)
                                    player!!.getPlayer()?.playWhenReady=false
                            }
                        }
                    }.build())
            }
            else audioManager!!.abandonAudioFocus(onAudioFocusChangeListener)
        }
        if (player!=null){
            player!!.releasePlayer()
            player=null
        }
    }

    override fun onBackPressed() {
        if (disableBackPress) return
        super.onBackPressed()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideSystemUI()
    }

    private fun hideSystemUI(){
        if (Build.VERSION.SDK_INT<=Build.VERSION_CODES.Q)
            playerView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        else {
            WindowCompat.setDecorFitsSystemWindows(window,false)
            WindowInsetsControllerCompat(window,window.decorView).let { c->
                c.hide(WindowInsetsCompat.Type.systemBars())
                c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }

    override fun showSubtitle(show: Boolean) {
        if (player==null||playerView.subtitleView==null) return
        if (!show){
            playerView.subtitleView?.visibility = View.GONE
            return
        }
        alertDialog.dismiss()
        //playerView.subtitleView?.visibility=View.VISIBLE
    }

    override fun changeSubtitleBackground() {
        val captionStyleCompat = CaptionStyleCompat(Color.YELLOW,Color.TRANSPARENT,Color.TRANSPARENT,CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,Color.LTGRAY,null)
        playerView.subtitleView?.setStyle(captionStyleCompat)
    }

    /*private fun checkForSubtitle(): Boolean {
        if (player?.getCurrentVideo()?.subtitles.isNullOrEmpty()) return true
        return false
    }

    private fun prepareSubtitles() {
        if (player==null||playerView.subtitleView==null) return
        if (checkForSubtitle()) {
            Toast.makeText(this,R.string.no_subtitle,Toast.LENGTH_SHORT).show()
            return
        }
        player?.pausePlayer()
        showSubtitleDialog()
    }

    private fun showSubtitleDialog() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN)
        val builder = AlertDialog.Builder(this,R.style.MyDialogTheme)
        val inflater = LayoutInflater.from(applicationContext)
        val view = inflater.inflate(R.layout.subtitle_selection_dialog,null)
        builder.setView(view)
        alertDialog=builder.create()
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window?.attributes)
        layoutParams.width=WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height=WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.gravity=Gravity.CENTER
        alertDialog.window?.attributes=layoutParams
        val recyclerView = view.findViewById<RecyclerView>(R.id.subtitle_recycler_view)
        recyclerView.adapter=SubtitleAdapter(player!!.getCurrentVideo().subtitles,player)
        val noSubtitle = view.findViewById<TextView>(R.id.no_subtitle_text_view)
        noSubtitle.setOnClickListener {
            if (playerView.subtitleView?.visibility==View.VISIBLE)
                showSubtitle(false)
            alertDialog.dismiss()
            player?.resumePlayer()
        }
        val cancelDialog = view.findViewById<Button>(R.id.cancel_dialog_btn)
        cancelDialog.setOnClickListener {
            alertDialog.dismiss()
            player?.resumePlayer()
        }
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
    }*/

    override fun setMuteMode(mute: Boolean) {
        if (player!=null||playerView!=null) {
            if (mute){
                muteBtn.visibility=View.GONE
                unmuteBtn.visibility=View.VISIBLE
            } else {
                unmuteBtn.visibility=View.GONE
                muteBtn.visibility=View.INVISIBLE
            }
        }
    }

    override fun showProgressBar(visible: Boolean) {
        progressBar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun updateLockMode(isLock: Boolean){
        if (player==null||playerView==null) return
        player?.isLock=isLock
        if (isLock){
            disableBackPress=true
            playerView.hideController()
            unlockBtn.visibility=View.VISIBLE
            return
        }
        disableBackPress=false
        playerView.showController()
        unlockBtn.visibility=View.GONE
    }

    override fun audioFocus() {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) {
            audioManager!!.requestAudioFocus(AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build()).setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener { focusChange->
                    when(focusChange) {
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,AudioManager.AUDIOFOCUS_LOSS->{
                            if (player!=null)
                                player!!.getPlayer()?.playWhenReady=false
                        }
                    }
                }.build())
        }
        else audioManager!!.abandonAudioFocus(onAudioFocusChangeListener)
    }

    override fun setVideoWatchedLength() {

    }

    override fun videoEnded() {

        player?.seekToNext()
    }

    override fun disableNextButtonOnLastVideo(disable: Boolean) {
        if (disable) {
            nextBtn.setImageResource(R.drawable.exo_disable_next_btn)
            nextBtn.isEnabled=false
            return
        }
        nextBtn.setImageResource(R.drawable.exo_next_btn)
        nextBtn.isEnabled=true
    }

    override fun onClick(v: View?) {
        val contId = v!!.id
        when(contId) {
            R.id.btn_mute->player?.setMute(true)
            R.id.btn_unMute->player?.setMute(false)
            R.id.btn_lock->updateLockMode(true)
            R.id.btn_unLock->updateLockMode(false)
            com.google.android.exoplayer2.ui.R.id.exo_rew->player?.seekToSelectedPosition(0,true)
            R.id.btn_back->onBackPressed()
            R.id.btn_next->player?.seekToNext()
            R.id.btn_prev->player?.seekToPrevious()
        }
    }

}