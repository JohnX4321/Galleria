package com.tzapps.galleria.fragments

import android.animation.Animator
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.adapters.ViewPagerAdapter
import com.tzapps.galleria.databinding.LayoutFragmentPagerBinding
import com.tzapps.galleria.utils.ExifUtils
import com.tzapps.galleria.utils.TagUtils
import com.tzapps.galleria.vm.GalleryViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.*

class ImageViewerFragment: Fragment() {

    private lateinit var _binding: LayoutFragmentPagerBinding
    val binding get() = _binding
    private val viewModel: GalleryViewModel by activityViewModels()
    private var isSystemUiVisible = true
    private var shortAnimDuration = 0L
    private var firstCurrentItem = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (!::_binding.isInitialized)
            _binding= LayoutFragmentPagerBinding.inflate(inflater,container,false)
        shortAnimDuration=resources.getInteger(android.R.integer.config_shortAnimTime).toLong()
        binding.tbViewPager.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        setupViewPager()
        prepareSharedElementTransition()
        setupViews()
        return binding.root
    }

    private fun setupViewPager(){
        val adapter = ViewPagerAdapter(this)
        if (requireArguments().getBoolean("isAlbum")) {
            adapter.submitList(viewModel.albums.value?.find { it.name== MainActivity.currentAlbumName }?.mediaItems)
            viewModel.albums.observe(viewLifecycleOwner){a->
                val items=a.find { it.name== MainActivity.currentAlbumName }?.mediaItems
                (binding.viewPager.adapter as ViewPagerAdapter).submitList(items)
            }
        } else {
            adapter.submitList(viewModel.viewPagerItems.value)
            viewModel.viewPagerItems.observe(viewLifecycleOwner){i->
                (binding.viewPager.adapter as ViewPagerAdapter).submitList(i)
            }
        }
        binding.viewPager.apply {
            this.adapter=adapter
            firstCurrentItem= MainActivity.currentPagerPos
            setCurrentItem(MainActivity.currentPagerPos,false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    MainActivity.currentPagerPos=position
                    MainActivity.currentListPos = if (requireArguments().getBoolean("isAlbum")) position else viewModel.viewPagerItems.value?.get(position)!!.listPosition
                }
            })
            setPageTransformer(MarginPageTransformer(50))
        }
    }

    fun showSystemUI() {
        binding.tbViewPager.apply {
            visibility= View.VISIBLE
            animate().alpha(1f).duration=shortAnimDuration
        }
        binding.cvEdit.apply {
            visibility= View.VISIBLE
            animate().alpha(1f).duration=shortAnimDuration
        }
        binding.cvShare.apply {
            visibility= View.VISIBLE
            animate().alpha(1f).duration=shortAnimDuration
        }
        binding.cvInfo.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimDuration
        }
        binding.cvDelete.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimDuration
        }
        binding.ivGradTop.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimDuration
        }
        binding.ivGardBottom.apply {
            visibility = View.VISIBLE
            animate().alpha(1f).duration = shortAnimDuration
        }
        WindowInsetsControllerCompat(requireActivity().window,requireActivity().window.decorView).show(
            WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemUI() {
        binding.tbViewPager.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {

                }

                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility= View.GONE
                }

                override fun onAnimationCancel(animation: Animator?) {

                }

                override fun onAnimationRepeat(animation: Animator?) {

                }
            })
        }
        binding.cvShare.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.cvEdit.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.cvInfo.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
            //   animate().alpha(0f).duration = shortAnimationDuration

        }
        binding.cvDelete.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.ivGradTop.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }
        binding.ivGardBottom.apply {
            animate().alpha(0f).setDuration(shortAnimDuration).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator?) = Unit
                override fun onAnimationEnd(animation: Animator?) {
                    if (!isSystemUiVisible) visibility = View.GONE
                }
                override fun onAnimationCancel(animation: Animator?) = Unit
                override fun onAnimationRepeat(animation: Animator?) = Unit
            })
        }

        WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
        }
    }

    fun toggleSystemUI() {
        if (isSystemUiVisible) hideSystemUI() else showSystemUI()
        isSystemUiVisible=!isSystemUiVisible
    }

    private fun setupSystemBars() {
        WindowInsetsControllerCompat(requireActivity().window,binding.root).let {
            it.isAppearanceLightStatusBars=false
            it.isAppearanceLightNavigationBars=false
        }
    }

    private fun setupViews() {
        ViewCompat.setOnApplyWindowInsetsListener(requireActivity().window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tbViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                topMargin = insets.top
            }
            binding.cvShare.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvEdit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvInfo.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvDelete.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            return@setOnApplyWindowInsetsListener windowInsets
        }
        binding.cvShare.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val share = Intent(Intent.ACTION_SEND)
            share.data = currentItem.uri
            share.type = activity?.contentResolver?.getType(currentItem.uri)
            share.putExtra(Intent.EXTRA_STREAM, currentItem.uri)
            share.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(share, "Share with"))
        }
        binding.cvDelete.setOnClickListener {
            if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.R) {
                viewModel.deleteExistingImage(getCurrentItem())
            } else {
                MaterialAlertDialogBuilder(
                    requireContext(),
                    R.style.Base_Theme_MaterialComponents_Dialog
                )
                    .setTitle("Permanently delete?")
                    .setMessage("This image will be permanently deleted.")
                    .setIcon(com.tzapps.galleria.R.drawable.ic_outline_delete_24)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        viewModel.deleteExistingImage(getCurrentItem())
                    }
                    .show()
            }
        }
        binding.cvEdit.setOnClickListener {
            val currentItem = getCurrentItem() ?: return@setOnClickListener
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.type = activity?.contentResolver?.getType(currentItem.uri)
            editIntent.data = currentItem.uri
            editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(editIntent, "Edit via"))
        }
        binding.cvInfo.setOnClickListener {
            showCurrentMediaDetails()
        }
    }

    private fun showCurrentMediaDetails() {
        val item = getCurrentItem()?:return
        val mediaCursor = requireActivity().contentResolver.query(item.uri, arrayOf(if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE),null,null)
        if (mediaCursor?.moveToFirst()!=true) {
            Toast.makeText(requireContext(),"Error occurred", Toast.LENGTH_LONG).show()
            mediaCursor?.close()
            return
        }

        val relPath=mediaCursor.getString(0)
        val fileName=mediaCursor.getString(1)
        val size=mediaCursor.getInt(2)
        mediaCursor.close()
        var dateAdded: String? = null
        var dateModified: String? = null
        var vfr = ExifUtils.NO_DATA
        var makeAndModel: String = ExifUtils.NO_DATA
        var dimension: String = ExifUtils.NO_DATA
        var aperture: String = ExifUtils.NO_DATA
        var fL: String = ExifUtils.NO_DATA
        var iso: String = ExifUtils.NO_DATA
        var exp: String = ExifUtils.NO_DATA
        var location: String = ExifUtils.NO_DATA
        var inp: InputStream? = null
        try {
            inp=requireActivity().contentResolver.openInputStream(item.uri)
            val exif = ExifInterface(inp!!)
            makeAndModel = TagUtils.getMakeAndModel(exif)
            dimension = TagUtils.getDimensions(exif)
            aperture = TagUtils.getAperture(exif)
            fL = TagUtils.getFocalLength(exif)
            iso = TagUtils.getISO(exif)
            exp = TagUtils.getExposure(exif)
            location = TagUtils.getLocation(exif)
            if (item.type==MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                val offset=exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)
                if (exif.hasAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                    dateAdded=convertTimeForPhoto(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)!!,offset)
                if (exif.hasAttribute(ExifInterface.TAG_DATETIME))
                    dateModified=convertTimeForPhoto(exif.getAttribute(ExifInterface.TAG_DATETIME)!!,offset)
            }
        } catch (e: IOException){
            e.printStackTrace()
        } finally {
            inp?.close()
        }

        if (item.type== MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
            val mmr= MediaMetadataRetriever()
            mmr.setDataSource(context,item.uri)
            val date=convertTimeForVideo(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)!!)
            dateAdded=date
            dateModified=date
            lifecycleScope.launch {
                vfr= TagUtils.getVideoFrameRate(relPath)
            }
        }
        val detBuilder = StringBuilder().apply {
            append("\nFile Name: \n$fileName\n\n")
            append("File Path: \n${getRelativePath(item.uri,relPath,fileName)}\n\n")
            append("File Size:\n")
            if (size==0) append("Loading...")
            else {
                append(String.format("%.2f",(size/(1024f*1024f))))
                append("mb")
            }
            append("\n\n")
            append("File Created: ")
            append(dateAdded ?: "N/A")
            append("\n\n")
            append("File Modified:")
            append(dateModified?: "N/A")
            append("\n\n")
            if (vfr!= ExifUtils.NO_DATA)
                append("Video FrameRate: $vfr\n\n")
            if (makeAndModel!= ExifUtils.NO_DATA)
                append("Captured On: $makeAndModel\n\n")
            if (location!= ExifUtils.NO_DATA)
                append("Location: $location\n\n")
            if (dimension!= ExifUtils.NO_DATA)
                append("Dimension: $dimension\n\n")
            if (aperture!= ExifUtils.NO_DATA)
                append("Aperture: $aperture\n\n")
            if (fL!= ExifUtils.NO_DATA)
                append("Focal Length: $fL\n\n")
            if (exp!= ExifUtils.NO_DATA)
                append("Exposure: $exp\n\n")
            if (iso!= ExifUtils.NO_DATA)
                append("ISO: $iso\n\n")
        }
        val bottomSheetDialog = BottomSheetDialog(requireContext()).apply {
            setTitle("Details")
            setContentView(com.tzapps.galleria.R.layout.layout_dialog_details)
            findViewById<TextView>(com.tzapps.galleria.R.id.detailsTextView)!!.apply {
                text=detBuilder.toString()
            }
        }
        bottomSheetDialog.show()
    }

    private fun getRelativePath(uri: Uri, path: String?, fileName: String): String {
        if (path==null) {
            val dPath= URLDecoder.decode(uri.lastPathSegment,"UTF-8")
            val sType=dPath.substring(0,7).replaceFirstChar { it.uppercase() }
            val rPath=dPath.substring(8)
            return "($sType Storage) $rPath"
        }
        return "(Primary Storage) $path$fileName"
    }

    private fun convertTime(time: Long,showTimeZone: Boolean = false): String {
        val date= Date(time)
        val format= SimpleDateFormat(if(showTimeZone) "yyy-MM-dd HH:mm:ss z" else "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault())
        format.timeZone= TimeZone.getDefault()
        return "${SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(date)} ${
            SimpleDateFormat.getTimeInstance(
                SimpleDateFormat.LONG).format(date)}"
    }

    private fun getCurrentItem() = if (requireArguments().getBoolean("isAlbum")) viewModel.albums.value?.find { it.name== MainActivity.currentAlbumName }?.mediaItems?.get(binding.viewPager.currentItem) else viewModel.viewPagerItems.value?.get(binding.viewPager.currentItem)

    private fun convertTimeForPhoto(time: String,offset: String? = null): String {
        val timestamp = if (offset!=null) "$time $offset" else time
        val dateFormat = SimpleDateFormat(if (offset==null) "yyyy:MM:dd HH:mm:ss" else "yyyy:MM:dd HH:mm:ss Z",
            Locale.getDefault()
        )
        if(offset==null) dateFormat.timeZone= TimeZone.getDefault()
        return convertTime(dateFormat.parse(timestamp)?.time?:0,offset!=null)
    }

    private fun convertTimeForVideo(time: String): String {
        val df = SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'", Locale.getDefault())
        df.timeZone= TimeZone.getTimeZone("UTC")
        return convertTime(df.parse(time)?.time?:0)
    }

    override fun startPostponedEnterTransition() {
        super.startPostponedEnterTransition()
        setupSystemBars()
    }

    private fun prepareSharedElementTransition() {
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            scrimColor = resources.getColor(android.R.color.black, requireActivity().theme)
        }

        setEnterSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {


                    val selectedViewHolder =
                        (binding.viewPager.getChildAt(0) as RecyclerView?)?.findViewHolderForAdapterPosition(binding.viewPager.currentItem)
                                as ViewPagerAdapter.ViewHolder? ?: return
                    sharedElements[names[0]] = selectedViewHolder.binding.pagerImage


                }
            })
        postponeEnterTransition()
    }

}