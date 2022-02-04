package com.tzapps.videoplayer.utils

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.Html
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckedTextView
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.Nullable
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Pair
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.RendererCapabilities
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.DefaultTrackNameProvider
import com.google.android.exoplayer2.ui.TrackNameProvider
import com.google.android.exoplayer2.util.Assertions

class PlayerTrackSelectionView(context: Context, @Nullable attrs: AttributeSet?,@AttrRes defStyleAttr: Int): LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        var currentBitrate = 0L
        const val BITRATE_1080P = 2800000
        const val BITRATE_720P = 1600000
        const val BITRATE_480P = 700000
        const val BITRATE_360P = 530000
        const val BITRATE_240P = 400000
        const val BITRATE_160P = 300000

        fun getDialog(activity: Activity,trackSelector: DefaultTrackSelector,rendererIndex: Int, currentBitrate: Long): Pair<AlertDialog,PlayerTrackSelectionView> {
            val builder = AlertDialog.Builder(activity)
            PlayerTrackSelectionView.currentBitrate = currentBitrate
            val dialogInflater = LayoutInflater.from(builder.context)
            val dialogView = dialogInflater.inflate(com.google.android.exoplayer2.ui.R.layout.exo_track_selection_dialog,null)
            val selectionView = dialogView.findViewById<PlayerTrackSelectionView>(com.google.android.exoplayer2.ui.R.id.exo_track_selection_view)
            selectionView.init(trackSelector,rendererIndex)
            val okClickListener = DialogInterface.OnClickListener  {d,w->
                selectionView.applySelection()
            }
            val dialog = builder.setView(dialogView)
                .setPositiveButton(android.R.string.ok,okClickListener).setNegativeButton(android.R.string.cancel,null).create()
            return Pair.create(dialog,selectionView)
        }

    }

    constructor(context: Context): this(context,null)
    constructor(context: Context,attrs: AttributeSet?): this(context,attrs,0)

    private var selectableItemBackgroundResourceId = -1
    private var inflater: LayoutInflater
    private var disableView: CheckedTextView
    private var defaultView: CheckedTextView
    private var componentListener: ComponentListener

    private var allowAdaptiveSelections = false
    private var trackNameProvider: TrackNameProvider
    private lateinit var trackViews: Array<Array<CheckedTextView>>

    private var trackSelector: DefaultTrackSelector? = null
    private var rendererIndex = -1
    private lateinit var trackGroups: TrackGroupArray
    private var isDisabled = false
    @Nullable
    private var override: DefaultTrackSelector.SelectionOverride? = null
    private val playingString = "<font color=#673AB7> &nbsp;(playing) &nbsp; </font>"

    init {
        val attrArray = context.theme.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
        selectableItemBackgroundResourceId=attrArray.getResourceId(0,0)
        attrArray.recycle()
        inflater= LayoutInflater.from(context)
        componentListener=ComponentListener()
        trackNameProvider=DefaultTrackNameProvider(resources)
        disableView=inflater.inflate(android.R.layout.simple_list_item_single_choice,this,false) as CheckedTextView
        disableView.apply {
            setText(com.google.android.exoplayer2.ui.R.string.exo_track_selection_none)
            isEnabled=false
            setBackgroundResource(selectableItemBackgroundResourceId)
            isFocusable=true
            setOnClickListener(componentListener)
            visibility=View.GONE
        }
        addView(disableView)
        addView(inflater.inflate(com.google.android.exoplayer2.ui.R.layout.exo_list_divider,this,false))
        defaultView=inflater.inflate(android.R.layout.simple_list_item_single_choice,this, false) as CheckedTextView
        defaultView.apply {
            setText(com.google.android.exoplayer2.ui.R.string.exo_track_selection_auto)
            setBackgroundResource(selectableItemBackgroundResourceId)
            isEnabled=false
            isFocusable=true
            setOnClickListener(componentListener)
        }
        addView(defaultView)
    }

    fun setAllowAdaptiveSelections(allowAdaptiveSelections: Boolean) {
        if (this.allowAdaptiveSelections!=allowAdaptiveSelections){
            this.allowAdaptiveSelections=allowAdaptiveSelections
            updateUI()
        }
    }

    fun setShowDisableOption(showDisableOption: Boolean)  {disableView.visibility = if (showDisableOption) View.VISIBLE else View.GONE}

    fun setTrackNameProvider(trackNameProvider: TrackNameProvider) {
        this.trackNameProvider=Assertions.checkNotNull(trackNameProvider)
        updateUI()
    }

    fun init(trackSelector: DefaultTrackSelector,rendererIndex: Int) {
        this.trackSelector=trackSelector
        this.rendererIndex=rendererIndex
        updateUI()
    }

    private fun updateUI() {
        for (i in childCount-1 downTo 3)
            removeViewAt(i)
        val trackerInfo = if (trackSelector==null) null else trackSelector!!.currentMappedTrackInfo
        if (trackSelector==null||trackerInfo==null){
            defaultView.isEnabled=false
            disableView.isEnabled=false
            return
        }

        disableView.isEnabled=true
        defaultView.isEnabled=true
        trackGroups=trackerInfo.getTrackGroups(rendererIndex)
        val params = trackSelector!!.parameters
        isDisabled=params.getRendererDisabled(rendererIndex)
        override=params.getSelectionOverride(rendererIndex,trackGroups)

        trackViews= emptyArray()
        for (groupIndex in 0 until trackGroups.length) {
            val group = trackGroups[groupIndex]
            var enabledAdaptiveSelections = allowAdaptiveSelections && trackGroups[groupIndex].length>1 && trackerInfo.getAdaptiveSupport(rendererIndex,groupIndex,false)!=RendererCapabilities.ADAPTIVE_NOT_SUPPORTED
            trackViews[groupIndex]= emptyArray()
            for (trackIndex in 0 until group.length) {
                if (trackIndex==0) addView(inflater.inflate(com.google.android.exoplayer2.ui.R.layout.exo_list_divider,this,false))
                val trackViewLayoutId = if (enabledAdaptiveSelections)
                    android.R.layout.simple_list_item_single_choice else android.R.layout.simple_list_item_multiple_choice
                val trackView = inflater.inflate(trackViewLayoutId,this,false) as CheckedTextView
                trackView.setBackgroundResource(selectableItemBackgroundResourceId)
                trackView.setText(Html.fromHtml(buildBitrateString(group.getFormat(trackIndex))))
                if (trackerInfo.getTrackSupport(rendererIndex, groupIndex, trackIndex)==RendererCapabilities.FORMAT_HANDLED){
                    trackView.isFocusable=true
                    trackView.tag=Pair.create(groupIndex,trackIndex)
                    trackView.setOnClickListener(componentListener)
                } else {
                    trackView.isFocusable=false
                    trackView.isEnabled=false
                }
                trackViews[groupIndex][trackIndex]=trackView
                addView(trackView)
            }
        }
        updateViewStates()
    }

    private fun updateViewStates(){
        disableView.isChecked=isDisabled
        defaultView.isChecked=!isDisabled&&override==null
        for (i in trackViews.indices)
            for (j in trackViews[i].indices)
                trackViews[i][j].isChecked=override!=null&&override!!.groupIndex==i&&override!!.containsTrack(j)
    }

    private fun applySelection(){
        val paramsBuilder = trackSelector!!.buildUponParameters()
        paramsBuilder.setRendererDisabled(rendererIndex,isDisabled)
        if (override!=null)
            paramsBuilder.setSelectionOverride(rendererIndex,trackGroups,override)
        else
            paramsBuilder.clearSelectionOverrides(rendererIndex)
        trackSelector!!.setParameters(paramsBuilder)
    }

    private fun onClick(view: View?) {
        if (view==disableView)
            onDisableViewClicked()
        else if (view==defaultView)
            onDefaultViewClicked()
        else
            onTrackViewClicked(view)
        updateViewStates()
    }

    private fun onDisableViewClicked(){
        isDisabled=true
        override=null
    }

    private fun onDefaultViewClicked(){
        isDisabled=false
        override=null
    }

    private fun onTrackViewClicked(view: View?) {
        if (view==null) return
        isDisabled=false
        val tag = view.tag as Pair<Int,Int>
        val groupIndex = tag.first
        val trackIndex = tag.second
        if (override==null)
            override=DefaultTrackSelector.SelectionOverride(groupIndex,trackIndex)
        else {
            val overrideTracks: IntArray = override?.tracks!!
            val tracks = getTracksRemoving(overrideTracks,override!!.tracks[0])
            override= DefaultTrackSelector.SelectionOverride(groupIndex,trackIndex)
        }
    }

    private fun getTracksAdding(tracks: IntArray, addedTracks: Int): IntArray {
        var tracks = tracks
        tracks= tracks.copyOf(tracks.size + 1)
        tracks[tracks.lastIndex] = addedTracks
        return tracks
    }

    private fun getTracksRemoving(tracks: IntArray,removedTrack: Int): IntArray {
        val newTracks = intArrayOf()
        var trackCount = 0
        for (track in tracks) {
            if (track!=removedTrack)
                newTracks[trackCount++]=track
        }
        return newTracks
    }

    private fun buildBitrateString(format: Format): String? {
        val bitrate: Int = format.bitrate
        val isPlaying = currentBitrate ==bitrate.toLong()
        if (bitrate == Format.NO_VALUE) {
            return updateText(isPlaying, trackNameProvider.getTrackName(format))
        }
        if (bitrate <= BITRATE_160P) {
            return updateText(isPlaying, " 160P")
        }
        if (bitrate <= BITRATE_240P) {
            return updateText(isPlaying, " 240P")
        }
        if (bitrate <= BITRATE_360P) {
            return updateText(isPlaying, " 360P")
        }
        if (bitrate <= BITRATE_480P) {
            return updateText(isPlaying, " 480P")
        }
        if (bitrate <= BITRATE_720P) {
            return updateText(isPlaying, " 720P")
        }
        return if (bitrate <= BITRATE_1080P) {
            updateText(isPlaying, " 1080P")
        } else trackNameProvider.getTrackName(format)
    }

    private fun updateText(isPlaying: Boolean, quality: String): String {
        return if (isPlaying) {
            if (!quality.contains(playingString)) quality + playingString else quality
        } else quality.replace(playingString, "")
    }




    private inner class ComponentListener: OnClickListener {
        override fun onClick(v: View?) {
            this@PlayerTrackSelectionView.onClick(v)
        }
    }

}