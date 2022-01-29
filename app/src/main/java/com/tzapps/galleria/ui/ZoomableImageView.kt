package com.tzapps.galleria.ui

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import com.tzapps.galleria.R
import com.tzapps.galleria.fragments.ImageViewerFragment
import kotlin.math.abs

class ZoomableImageView @JvmOverloads constructor(context: Context,attrs: AttributeSet? = null): AppCompatImageView(context, attrs) {

    companion object {
        const val NONE=0
        const val DRAG=1
        const val ZOOM=2
        const val CLICK=3
    }

    lateinit var mMatrix : Matrix

    private var mode = NONE

    private var last = PointF()
    private var start = PointF()
    private var minScale = 1f
    private var maxScale = 3f
    private var m: FloatArray = FloatArray(9)
    private var viewWidth = 0
    var viewHeight = 0
    private var saveScale = 1f
    private var origWidth = 0f
    var origHeight = 0f
    private var oldMeasuredWidth = 0
    private var oldMeasuredHeight = 0
    private var mScaleDetector: ScaleGestureDetector? = null

    private var isZoomingDisabled = true

    //  lateinit var gActivity : InAppGallery
    lateinit var gFrag: ImageViewerFragment

    init {
        sharedConstructing()
    }

    private val currentInstance : ZoomableImageView
        get() {
            return gFrag.binding.viewPager.getChildAt(0).findViewById(R.id.pagerImage)
        }


    fun setViewPagerFrag(frag: ImageViewerFragment) {
        this.gFrag = frag
    }

    fun enableZooming() {
        isZoomingDisabled = false
    }

    fun disableZooming() {
        isZoomingDisabled = true
    }

    private fun sharedConstructing() {
        super.setClickable(true)
        mScaleDetector= ScaleGestureDetector(context,ScaleListener())
        mMatrix= Matrix()
        imageMatrix=mMatrix
        scaleType=ScaleType.MATRIX

        setOnTouchListener { v, event ->
            currentInstance.mScaleDetector!!.onTouchEvent(event)
            if (currentInstance.isZoomingDisabled){
                if (event.action==MotionEvent.ACTION_UP)
                    return@setOnTouchListener performClick()
                return@setOnTouchListener false
            }
            val curr = PointF(event.x,event.y)
            when(event.action) {
                MotionEvent.ACTION_DOWN->{
                    currentInstance.last.set(curr)
                    currentInstance.start.set(currentInstance.last)
                    currentInstance.mode= DRAG
                }
                MotionEvent.ACTION_MOVE->if (currentInstance.mode== DRAG) {
                    val deltaX=curr.x-currentInstance.last.x
                    val deltaY=curr.y-currentInstance.last.y

                    val transFixX=currentInstance.getFixDragTrans(deltaX,currentInstance.viewWidth.toFloat(),currentInstance.origWidth*currentInstance.saveScale)
                    val transFixY=currentInstance.getFixDragTrans(deltaY,currentInstance.viewHeight.toFloat(),currentInstance.origHeight*currentInstance.saveScale)
                    currentInstance.mMatrix.postTranslate(transFixX,transFixY)
                    currentInstance.fixTrans()
                    currentInstance.last[curr.x]=curr.y
                }
                MotionEvent.ACTION_UP->{
                    currentInstance.mode= NONE
                    val xDiff= abs(curr.x-currentInstance.start.x).toInt()
                    val yDiff= abs(curr.y-currentInstance.start.y).toInt()
                    if (xDiff< CLICK&&yDiff< CLICK) performClick()
                }
                MotionEvent.ACTION_POINTER_UP->{
                    currentInstance.mode= NONE
                }
            }
            currentInstance.imageMatrix=currentInstance.mMatrix
            currentInstance.invalidate()
            true
        }
    }

    private var isInZoomMode = false

    fun moveIntoZoom(){
        if (isInZoomMode) return
        isInZoomMode = true
        gFrag.let {
            it.hideSystemUI()
            it.binding.viewPager.isUserInputEnabled = false
        }
    }

    fun mouveOufOfZoom() {
        if (!isInZoomMode) return
        isInZoomMode=false
        gFrag.let {
            it.showSystemUI()
            it.binding.viewPager.isUserInputEnabled=true
        }
    }

    fun fixTrans() {
        mMatrix.getValues(m)
        val transX=m[Matrix.MTRANS_X]
        val transY=m[Matrix.MTRANS_Y]
        val transFixX=getFixTrans(transX,viewWidth.toFloat(),origWidth*saveScale)
        val transFixY=getFixTrans(transY,viewHeight.toFloat(),origHeight*saveScale)
        if (transFixX!=0f||transFixY!=0f) mMatrix.postTranslate(transFixX,transFixY)
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize<=viewSize){
            minTrans=0f
            maxTrans=viewSize-contentSize
        } else {
            minTrans=viewSize-contentSize
            maxTrans=0f
        }
        if (trans<minTrans) return  -trans+minTrans
        return if (trans>maxTrans) -trans+maxTrans else 0f
    }

    private fun getFixDragTrans(delta: Float,viewSize: Float,contentSize: Float) = if (contentSize<=viewSize) 0f else delta

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth=MeasureSpec.getSize(widthMeasureSpec)
        viewHeight=MeasureSpec.getSize(heightMeasureSpec)
        if (oldMeasuredHeight==viewWidth&&oldMeasuredHeight==viewHeight||viewWidth==0||viewHeight==0) return
        oldMeasuredHeight=viewHeight
        oldMeasuredWidth=viewWidth
        if (saveScale==1f) {
            val scale: Float
            if (drawable?.intrinsicWidth?:0==0||drawable?.intrinsicHeight?:0==0) return
            val bmWidth=drawable.intrinsicWidth
            val bmHeight=drawable.intrinsicHeight
            val scaleX=viewWidth.toFloat()/bmWidth.toFloat()
            val scaleY=viewHeight.toFloat()/bmHeight.toFloat()
            scale=scaleX.coerceAtMost(scaleY)
            mMatrix.setScale(scale,scale)

            var redundantSpaceY=viewHeight.toFloat()-scale*bmHeight.toFloat()
            var redundantSpaceX=viewWidth.toFloat()-scale*bmWidth.toFloat()
            redundantSpaceX/=2f
            redundantSpaceY/=2f
            mMatrix.postTranslate(redundantSpaceX,redundantSpaceY)
            origWidth=viewWidth-2*redundantSpaceX
            origHeight=viewHeight-2*redundantSpaceY
            imageMatrix=mMatrix
        }
        fixTrans()
    }

    private inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            mode= ZOOM
            if (isZoomingDisabled) gFrag.showSystemUI()
            else gFrag.binding.viewPager.isUserInputEnabled=false
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (isZoomingDisabled) return true
            var mScaleFactor=detector.scaleFactor
            val origScale=saveScale
            saveScale*=mScaleFactor
            if (saveScale>maxScale){
                saveScale=maxScale
                mScaleFactor=maxScale/origScale
            } else if (saveScale<minScale){
                saveScale=minScale
                mScaleFactor=minScale/origScale
            }
            if (origWidth*saveScale<=viewWidth||origHeight*saveScale<=viewHeight) mMatrix.postScale(mScaleFactor,mScaleFactor,viewWidth/2f,viewHeight/2f)
            else mMatrix.postScale(mScaleFactor,mScaleFactor,detector.focusX,detector.focusY)
            fixTrans()
            if (saveScale==1f) mouveOufOfZoom()
            else moveIntoZoom()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            if (saveScale==1f) gFrag.binding.viewPager.isUserInputEnabled=true
        }
    }



}