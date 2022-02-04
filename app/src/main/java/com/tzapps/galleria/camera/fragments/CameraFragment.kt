package com.tzapps.galleria.camera.fragments

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.Camera
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Size
import android.view.GestureDetector
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.core.ImageCapture.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.tzapps.galleria.R
import com.tzapps.galleria.camera.ext.*
import com.tzapps.galleria.camera.utils.CameraTimer
import com.tzapps.galleria.camera.utils.LuminosityAnalyzer
import com.tzapps.galleria.camera.utils.SwipeGestureDetector
import com.tzapps.galleria.camera.utils.ThreadExecutor
import com.tzapps.galleria.databinding.LayoutCameraFragmentBinding
import com.tzapps.galleria.utils.Prefs
import com.tzapps.galleria.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.properties.Delegates

class CameraFragment: BaseFragment<LayoutCameraFragmentBinding>(R.layout.layout_camera_fragment) {


    override val binding: LayoutCameraFragmentBinding by lazy { LayoutCameraFragmentBinding.inflate(layoutInflater) }

    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null

    private var displayId = -1
    private var lensFacing = CameraSelector.DEFAULT_BACK_CAMERA
    private var hdrCameSelector: CameraSelector? = null

    private var flashMode by Delegates.observable(FLASH_MODE_OFF) {_,_,n->
        binding.btnFlash.setImageResource(
            when(n){
                FLASH_MODE_ON->R.drawable.ic_flash_on
                FLASH_MODE_OFF->R.drawable.ic_flash_auto
                else->R.drawable.ic_flash_off
            }
        )
    }

    private val displayManager by lazy { requireContext().getSystemService(DisplayManager::class.java) }

    private var hasGrid = false
    private var hasHdr = false
    private var selectedTimer = CameraTimer.OFF

    /**
     * A display listener for orientation changes that do not trigger a configuration
     * change, for example if we choose to override config change in manifest or for 180-degree
     * orientation changes.
     */
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {

        }

        override fun onDisplayRemoved(displayId: Int) {

        }

        override fun onDisplayChanged(displayId: Int) {
            view?.let { v->
                if (displayId==this@CameraFragment.displayId){
                    preview?.targetRotation=v.display.rotation
                    imageCapture?.targetRotation=v.display.rotation
                    imageAnalyzer?.targetRotation=v.display.rotation
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        flashMode=Prefs.getFlashMode()
        hasGrid=Prefs.hasGrid()
        hasHdr=Prefs.hasHDR()
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
                    displayManager.registerDisplayListener(displayListener,null)
                }
            })
            btnTakePicture.setOnClickListener { capturePicture() }
            btnSwitchCamera.setOnClickListener { toggleCamera() }
            btnTimer.setOnClickListener { selectTimer() }
            btnGrid.setOnClickListener { toggleGrid() }
            btnHdr.setOnClickListener { toggleHDR() }
            btnFlash.setOnClickListener { selectFlash() }
            btnTimerOff.setOnClickListener { configTimer(CameraTimer.OFF) }
            btnTimer3.setOnClickListener { configTimer(CameraTimer.S3) }
            btnTimer10.setOnClickListener { configTimer(CameraTimer.S10) }
            btnFlashOff.setOnClickListener { configFlash(FLASH_MODE_OFF) }
            btnFlashOn.setOnClickListener { configFlash(FLASH_MODE_ON) }
            btnFlashAuto.setOnClickListener { configFlash(FLASH_MODE_AUTO) }
            btnExposure.setOnClickListener { flExposure.visibility = View.VISIBLE }
            flExposure.setOnClickListener { flExposure.visibility = View.GONE }

            val swipeGestures = SwipeGestureDetector().apply {
                setSwipeCallback(right = {
                    Navigation.findNavController(view).navigate(R.id.action_camera_to_video)
                })
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
        binding.btnTakePicture.onWindowInsets { view, windowInsetsCompat ->
            if (resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT)
                view.bottomMargin=windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            else
                view.endMargin=windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).top
        }
        binding.btnTimer.onWindowInsets { view, windowInsetsCompat -> view.topMargin=windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).top }
        binding.llTimerOptions.onWindowInsets { view, windowInsetsCompat ->
            if (resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT)
                view.topPadding = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).top
            else
                view.startPadding = windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).left
        }
        binding.llFlashOptions.onWindowInsets { view, windowInsetsCompat ->
            if(resources.configuration.orientation==Configuration.ORIENTATION_PORTRAIT)
                view.topPadding=windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).top
            else
                view.startPadding=windowInsetsCompat.getInsets(WindowInsetsCompat.Type.systemBars()).left
        }
    }

    fun toggleCamera() = binding.btnSwitchCamera.toggleButton(flag = lensFacing== CameraSelector.DEFAULT_BACK_CAMERA,rotationAngle = 180f,firstIcon = R.drawable.ic_outline_camera_rear,secondIcon = R.drawable.ic_outline_camera_front) {
        lensFacing = if (it) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
        startCamera()
    }



    private fun selectTimer() = binding.llTimerOptions.circularReveal(binding.btnTimer)

    private fun configTimer(timer: CameraTimer) = binding.llTimerOptions.circularClose(binding.btnTimer) {
        selectedTimer=timer
        binding.btnTimer.setImageResource(when (timer) {
            CameraTimer.S3->R.drawable.ic_timer_3
            CameraTimer.S10->R.drawable.ic_timer_10
            CameraTimer.OFF->R.drawable.ic_timer_off
        })
    }

    private fun selectFlash() = binding.llFlashOptions.circularReveal(binding.btnFlash)

    private fun configFlash(@FlashMode flash: Int) = binding.llFlashOptions.circularClose(binding.btnFlash) {
        flashMode=flash
        binding.btnFlash.setImageResource(when(flash){
            FLASH_MODE_ON->R.drawable.ic_flash_on
            FLASH_MODE_OFF->R.drawable.ic_flash_off
            else->R.drawable.ic_flash_auto
        })
    }

    private fun toggleGrid() = binding.btnGrid.toggleButton(flag = hasGrid,rotationAngle = 180f,firstIcon = R.drawable.ic_grid_off,secondIcon = R.drawable.ic_grid_on) {f->
        hasGrid=f
        Prefs.setGrid(f)
        binding.groupGridLines.visibility = if (f) View.VISIBLE else View.GONE
    }

    private fun toggleHDR() = binding.btnHdr.toggleButton(flag = hasHdr,rotationAngle = 360f,firstIcon = R.drawable.ic_hdr_off,secondIcon = R.drawable.ic_hdr_on) {f->
        hasHdr=f
        Prefs.setHDR(f)
        startCamera()
    }

    override fun onPermGranted() {
        binding.viewFinder.let { vf->
            vf.post {
                displayId=vf.display.displayId
                startCamera()
            }
        }
    }

    private fun startCamera() {
        val vf = binding.viewFinder
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider=cameraProviderFuture.get()
            } catch (e: Exception){
                e.printStackTrace()
                Toast.makeText(requireContext(),"Error Starting Camera",Toast.LENGTH_SHORT).show()
                return@addListener
            }
            val size: Size
            if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) requireContext().getSystemService(WindowManager::class.java).currentWindowMetrics.apply {
                size=Size(bounds.width(),bounds.height())
            } else {
                Resources.getSystem().displayMetrics.apply {
                    size=Size(widthPixels,heightPixels)
                }
            }
            val aspectRatio = aspectRatio(size.width,size.height)
            val rotation = vf.display.rotation
            val localCameraProvider = cameraProvider?: throw Exception("Camera Init failed")
            preview = Preview.Builder().setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation).build()
            imageCapture=ImageCapture.Builder().setCaptureMode(CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(flashMode).setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation).build()
            checkForHDR()
            imageAnalyzer = ImageAnalysis.Builder().setTargetAspectRatio(aspectRatio)
                .setTargetRotation(rotation).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build().also { setLuminosityAnalyzer(it) }
            //localCameraProvider.unbindAll()
            bindToLifeCycle(localCameraProvider,vf)
        },ContextCompat.getMainExecutor(requireContext()))
    }

    private fun checkForHDR() {
        val camProvider = cameraProvider ?: return
        val extensionsManager = ExtensionsManager.getInstanceAsync(requireContext(),cameraProvider!!)
        extensionsManager.addListener({
            val extMgr = extensionsManager.get() ?: return@addListener
            val isAvailable = extMgr.isExtensionAvailable(lensFacing,ExtensionMode.HDR)
            if (!isAvailable)
                binding.btnHdr.visibility = View.GONE
            else if (hasHdr){
                binding.btnHdr.visibility = View.VISIBLE
                hdrCameSelector = extMgr.getExtensionEnabledCameraSelector(lensFacing,ExtensionMode.HDR)
            }
        },ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setLuminosityAnalyzer(imageAnalysis: ImageAnalysis) {
        val analyzerThread = HandlerThread("LuminosityAnalyzer").apply { start() }
        imageAnalysis.setAnalyzer(ThreadExecutor(Handler(analyzerThread.looper)),LuminosityAnalyzer())
    }

    private fun bindToLifeCycle(localCameraProvider: ProcessCameraProvider, viewFinder: PreviewView) {
        try {
            localCameraProvider.bindToLifecycle(viewLifecycleOwner,hdrCameSelector?:lensFacing,preview,imageCapture,imageAnalyzer).run {
                cameraInfo.exposureState.run {
                    val lower = exposureCompensationRange.lower
                    val upper = exposureCompensationRange.upper
                    binding.sliderExposure.run {
                        valueFrom=lower.toFloat()
                        valueTo=upper.toFloat()
                        stepSize=1f
                        value=exposureCompensationIndex.toFloat()
                        addOnChangeListener { _, value, _ -> cameraControl.setExposureCompensationIndex(value.toInt()) }
                    }
                }
            }
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun aspectRatio(width: Int,height: Int): Int {
        val pR=max(width,height).toDouble()/min(width,height)
        if (abs(pR-Utils.RATIO_4_3)<= abs(pR-Utils.RATIO_16_9))
            return AspectRatio.RATIO_4_3
        return AspectRatio.RATIO_16_9
    }

    private fun capturePicture() = lifecycleScope.launch(Dispatchers.Main) {
        when(selectedTimer){
            CameraTimer.S3-> for (i in 3 downTo 1){
                binding.tvCountDown.text=i.toString()
                delay(1000)
            }
            CameraTimer.S10-> for (i in 10 downTo 1){
                binding.tvCountDown.text=i.toString()
                delay(1000)
            }
        }
        binding.tvCountDown.text=""
        captureImage()
    }

    private fun captureImage() {
        val localImageCapture = imageCapture?: throw Exception()
        val metadata = Metadata().apply {
            isReversedHorizontal=lensFacing== CameraSelector.DEFAULT_FRONT_CAMERA
        }
        val outputCV = if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) {
            val cv=ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME,"${System.currentTimeMillis()}_CX.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE,"image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH,outputDirectory)
            }
            OutputFileOptions.Builder(requireContext().contentResolver,MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),cv)
        } else {
            File(outputDirectory).mkdirs()
            val f=File(outputDirectory,"${System.currentTimeMillis()}_CX.jpg")
            OutputFileOptions.Builder(f)
        } .setMetadata(metadata).build()
        localImageCapture.takePicture(outputCV,requireContext().mainExecutor(),object : OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: OutputFileResults) {
                Toast.makeText(requireContext(),"Saved at ${outputFileResults.savedUri}",Toast.LENGTH_SHORT).show()
            }

            override fun onError(exception: ImageCaptureException) {
                val msg = "Photo capture failed: ${exception.message}"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                exception.printStackTrace()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onBackPressed() {
        when {
            binding.llTimerOptions.visibility == View.VISIBLE-> binding.llTimerOptions.circularClose(binding.btnTimer)
            binding.llFlashOptions.visibility == View.VISIBLE-> binding.llFlashOptions.circularClose(binding.btnFlash)
            else-> requireActivity().finish()
        }
    }



}