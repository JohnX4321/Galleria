package com.tzapps.galleria.fragments

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.app.SharedElementCallback
import androidx.core.view.ViewGroupCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.R
import com.tzapps.galleria.activities.CameraActivity
import com.tzapps.galleria.activities.InfoActivity
import com.tzapps.galleria.adapters.GridItemAdapter
import com.tzapps.galleria.databinding.LayoutFragmentNavBinding

class BottomNavFragment: Fragment() {

    private lateinit var _binding: LayoutFragmentNavBinding
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!::_binding.isInitialized)
            _binding= LayoutFragmentNavBinding.inflate(inflater,container,false)
        val frag=childFragmentManager.findFragmentById(R.id.fcvBottomNav)
        if (frag is GridItemFragment) {
            postponeEnterTransition()
            prepareTransitions()
        }

        binding.appBarLayout.statusBarForeground=MaterialShapeDrawable.createWithElevationOverlay(requireContext())
        binding.fcvBottomNav.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin=binding.bnvMain.measuredHeight }
        binding.bnvMain.setOnItemReselectedListener {
            val f=childFragmentManager.findFragmentById(R.id.fcvBottomNav)
            if (f is GridItemFragment) f.binding.rvItems.smoothScrollToPosition(0)
            else if (f is GridAlbumFragment) f.binding.rvAlbum.smoothScrollToPosition(0)
        }
        binding.bnvMain.setOnItemSelectedListener {
            when(it.itemId) {
                R.id.item_gallery->{
                    childFragmentManager.commit {
                        replace<GridItemFragment>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                        MainActivity.currentListPos=0
                    }
                    true
                }
                R.id.item_albums->{
                    if (frag is GridItemFragment && frag.actionMode!=null) {
                        frag.actionMode?.finish()
                        frag.actionMode = null
                    }
                    childFragmentManager.commit {
                        replace<GridAlbumFragment>(R.id.fcvBottomNav)
                        setReorderingAllowed(true)
                    }
                    true
                }
                R.id.item_menu->{
                    val bottomSheetDialog = BottomSheetDialog(requireContext()).apply {
                        setContentView(R.layout.layout_menu_dialog)
                        findViewById<ImageButton>(R.id.item_settings)!!.setOnClickListener {
                            if (frag is GridItemFragment && frag.actionMode!=null) {
                                frag.actionMode?.finish()
                                frag.actionMode = null
                            }
                            MainActivity.isMoreOpened=true
                            childFragmentManager.commit {
                                replace<SettingsFragment>(R.id.fcvBottomNav)
                                addToBackStack(null)
                                dismiss()
                            }
                        }
                        findViewById<ImageButton>(R.id.item_info)!!.setOnClickListener {
                            startActivity(Intent(requireContext(),InfoActivity::class.java))
                        }
                        findViewById<ImageButton>(R.id.item_camera)!!.setOnClickListener {
                            startActivity(Intent(requireContext(),CameraActivity::class.java))
                        }
                    }
                    bottomSheetDialog.setOnDismissListener {
                        if (!MainActivity.isMoreOpened) {
                            binding.bnvMain.selectedItemId=R.id.item_gallery
                        }
                    }
                    bottomSheetDialog.show()
                    false
                }
                else -> false
            }
        }
        return binding.root
    }

    fun startActionMode(callback: ActionMode.Callback): ActionMode {
        return binding.tbMain.startActionMode(callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareTransitions()
    }

    private fun setupSystemBars() {
        val nightModeFlags: Int = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
            nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED) {
            WindowInsetsControllerCompat(requireActivity().window, binding.root).let { controller ->
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setupSystemBars()
    }

    fun prepareTransitions() {
        exitTransition = if (enteringFromAlbum) MaterialSharedAxis(MaterialSharedAxis.Z,false) else Hold()

        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>?,
                sharedElements: MutableMap<String, View>?
            ) {
                val frag = childFragmentManager.findFragmentById(R.id.fcvBottomNav) as GridItemFragment?
                if (frag!=null){
                    ViewGroupCompat.setTransitionGroup(frag.binding.rvItems,false)
                }
                val selectedVH = frag?.binding?.rvItems?.findViewHolderForAdapterPosition(MainActivity.currentListPos) ?: return
                sharedElements?.set(names!![0],
                    (selectedVH as GridItemAdapter.MediaItemHolder).binding.image
                )
            }
        })
    }

    companion object {
        var enteringFromAlbum = false
    }

}