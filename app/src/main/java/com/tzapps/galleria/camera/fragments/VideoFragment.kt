package com.tzapps.galleria.camera.fragments

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.animation.doOnCancel
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.Navigation
import com.tzapps.galleria.R
import com.tzapps.galleria.camera.ext.*
import com.tzapps.galleria.camera.utils.SwipeGestureDetector
import com.tzapps.galleria.databinding.LayoutCameraFragmentVideoBinding
import com.tzapps.galleria.utils.Prefs
import com.tzapps.galleria.utils.Utils
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class VideoFragment: BaseFragment<LayoutCameraFragmentVideoBinding>(R.layout.layout_camera_fragment_video) {

    private val displayManager by lazy { requireContext().getSystemService(DisplayManager::class.java) }

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture? = null
    private var displayId = -1
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA

    private var flashMode by Delegates.observable(ImageCapture.FLASH_MODE_OFF) {_,_,n->
        binding.btnFlash.setImageResource(
            when (n) {
                ImageCapture.FLASH_MODE_ON -> R.drawable.ic_flash_on
                ImageCapture.FLASH_MODE_AUTO-> R.drawable.ic_flash_auto
                else-> R.drawable.ic_flash_off
            }
        )
    }
    private var hasGrid = false
    private var isTorchOn = false
    private var isRecording = false
    private val recordAnimation by lazy {
        ObjectAnimator.ofFloat(binding.btnRecordVideo,View.ALPHA,1f,0.5f).apply {
            repeatMode=ObjectAnimator.REVERSE
            repeatCount=ObjectAnimator.INFINITE
            doOnCancel { binding.btnRecordVideo.alpha=1f }
        }
    }

    override val binding: LayoutCameraFragmentVideoBinding by lazy { LayoutCameraFragmentVideoBinding.inflate(layoutInflater) }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {

        }

        override fun onDisplayRemoved(displayId: Int) {

        }

        @SuppressLint("RestrictedApi")
        override fun onDisplayChanged(displayId: Int) {
            view?.let { v->
                if (displayId==this@VideoFragment.displayId){
                    preview?.targetRotation=v.display.rotation
                    videoCapture?.setTargetRotation(v.display.rotation)
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasGrid=Prefs.hasGrid()
        binding.btnGrid.setImageResource(if (hasGrid) R.drawable.ic_grid_on else R.drawable.ic_grid_off)
        binding.groupGridLines.visibility = if (hasGrid) View.VISIBLE else View.GONE
        adjustInsets()
        displayManager.registerDisplayListener(displayListener,null)
        binding.run {
            viewFinder.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View?) {
                    displayManager.unregisterDisplayListener(displayListener)
                }

                override fun onViewDetachedFromWindow(v: View?) {
                    displayManager.registerDisplayListener(displayListener, null)
                }
            })
            binding.btnRecordVideo.setOnClickListener { recordVideo() }
            btnSwitchCamera.setOnClickListener { toggleCamera() }
            btnGrid.setOnClickListener { toggleGrid() }
            btnFlash.setOnClickListener { toggleFlash() }
            val swipeGestures = SwipeGestureDetector().apply {
                setSwipeCallback(left = {Navigation.findNavController(view).navigate(R.id.action_video_to_camera)})
            }
            val gestureDetectorCompat = GestureDetector(requireContext(),swipeGestures)
            viewFinder.setOnTouchListener { _, event ->
                if (gestureDetectorCompat.onTouchEvent(event)) return@setOnTouchListener false
                return@setOnTouchListener true
            }
        }
    }

    private fun adjustInsets() {
        activity?.window?.fitsSystemWindows()
        binding.btnRecordVideo.onWindowInsets { view, windowInsets ->
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                view.bottomMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            } else {
                view.endMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right
            }
        }
        binding.btnFlash.onWindowInsets { view, windowInsets ->
            view.topMargin = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top
        }
    }

    private fun toggleCamera() = binding.btnSwitchCamera.toggleButton(
        flag = lensFacing == CameraSelector.DEFAULT_BACK_CAMERA,
        rotationAngle = 180f,
        firstIcon = R.drawable.ic_outline_camera_rear,
        secondIcon = R.drawable.ic_outline_camera_front,
    ) {
        lensFacing = if (it) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        startCamera()
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        val vf = binding.viewFinder
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider=cameraProviderFuture.get()
            } catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(),"Error Starting Camera", Toast.LENGTH_SHORT).show()
                return@addListener
            }
            val size: Size
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.R) requireContext().getSystemService(
                WindowManager::class.java).currentWindowMetrics.apply {
                size= Size(bounds.width(),bounds.height())
            } else {
                Resources.getSystem().displayMetrics.apply {
                    size= Size(widthPixels,heightPixels)
                }
            }
            val aspectRatio = aspectRatio(size.width,size.height)
            val rotation = vf.display.rotation
            val localCameraProvider = cameraProvider?: throw Exception("Camera Init failed")
            preview = Preview.Builder().setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation).build()
            val videoCaptureConfig = VideoCapture.DEFAULT_CONFIG.config
            videoCapture=VideoCapture.Builder.fromConfig(videoCaptureConfig).build()

            //localCameraProvider.unbindAll()
            try {
                camera=localCameraProvider.bindToLifecycle(viewLifecycleOwner,lensFacing,preview,videoCapture)
                preview?.setSurfaceProvider(vf.surfaceProvider)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - Utils.RATIO_4_3) <= abs(previewRatio - Utils.RATIO_16_9)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    @SuppressLint("RestrictedApi","MissingPermission")
    private fun recordVideo() {
        val localVideoCapture = videoCapture?: throw Exception()

        val outputCV = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
            val cv= ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,"${System.currentTimeMillis()}_CX.mp4")
                put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4")
                put(MediaStore.MediaColumns.RELATIVE_PATH,outputDirectory)
            }
            VideoCapture.OutputFileOptions.Builder(requireContext().contentResolver,
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),cv)
        } else {
            File(outputDirectory).mkdirs()
            val f= File(outputDirectory,"${System.currentTimeMillis()}_CX.mp4")
            VideoCapture.OutputFileOptions.Builder(f)
        }.build()
        if (!isRecording) {
            recordAnimation.start()
            localVideoCapture.startRecording(outputCV,requireContext().mainExecutor(),object : VideoCapture.OnVideoSavedCallback {
                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    recordAnimation.cancel()
                    val msg = "Video capture failed: $message"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                    cause?.printStackTrace()
                }

                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {

                }
            })
        } else {
            recordAnimation.cancel()
            localVideoCapture.stopRecording()
        }
        isRecording=!isRecording
    }

    /**
     * Turns on or off the grid on the screen
     * */
    private fun toggleGrid() = binding.btnGrid.toggleButton(
        flag = hasGrid,
        rotationAngle = 180f,
        firstIcon = R.drawable.ic_grid_off,
        secondIcon = R.drawable.ic_grid_on
    ) { flag ->
        hasGrid = flag
        Prefs.setGrid(hasGrid)
        binding.groupGridLines.visibility = if (flag) View.VISIBLE else View.GONE
    }

    /**
     * Turns on or off the flashlight
     * */
    private fun toggleFlash() = binding.btnFlash.toggleButton(
        flag = flashMode == ImageCapture.FLASH_MODE_ON,
        rotationAngle = 360f,
        firstIcon = R.drawable.ic_flash_off,
        secondIcon = R.drawable.ic_flash_on
    ) { flag ->
        isTorchOn = flag
        flashMode = if (flag) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flag)
    }


    override fun onBackPressed() = requireActivity().finish()

    override fun onStop() {
        super.onStop()
        camera?.cameraControl?.enableTorch(false)
    }



}