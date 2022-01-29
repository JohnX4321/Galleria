package com.tzapps.galleria.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.core.app.SharedElementCallback
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.adapters.GridItemAdapter
import com.tzapps.galleria.databinding.LayoutFragmentAlbumBinding
import com.tzapps.galleria.models.ListItem
import com.tzapps.galleria.utils.CustomItemDetailsLookup
import com.tzapps.galleria.vm.GalleryViewModel

class AlbumDetailFragment: Fragment() {

    private lateinit var _binding: LayoutFragmentAlbumBinding
    private val binding get() = _binding
    private val viewModel: GalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding= LayoutFragmentAlbumBinding.inflate(inflater,container,false)
        val adapter = GridItemAdapter(this,true)
        binding.rvAlbums.apply {
            setHasFixedSize(true)
            this.adapter=adapter
        }

        val tracker = SelectionTracker.Builder("GridItemFragmentSelectionId",binding.rvAlbums,StableIdKeyProvider(binding.rvAlbums),CustomItemDetailsLookup(binding.rvAlbums),
            StorageStrategy.createLongStorage()).build()
        adapter.tracker=tracker
        BottomNavFragment.enteringFromAlbum=true

        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                WindowInsetsControllerCompat(requireActivity().window,binding.root).isAppearanceLightStatusBars = false
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker.clearSelection()
                WindowInsetsControllerCompat(requireActivity().window,binding.root).isAppearanceLightStatusBars = false
            }
        }

        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            var actionMode: ActionMode? = null
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                actionMode?.title = tracker.selection.size().toString()
                if (actionMode==null)
                    actionMode=binding.tbAlbum.startActionMode(callback)
                else if (tracker.selection.size()==0){
                    actionMode?.finish()
                    actionMode==null
                }
            }
        })

        viewModel.albums.observe(viewLifecycleOwner) { a ->
            val items = a.find { it.name == MainActivity.currentAlbumName }?.mediaItems
            val position =
                (binding.rvAlbums.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            adapter.submitList(items as List<ListItem>) {
                if (position == 0) binding.rvAlbums.scrollToPosition(0)
            }
        }

        binding.tbAlbum.title=MainActivity.currentAlbumName

        binding.tbAlbum.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        prepareTransitions()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setupSystemBars()
        viewModel.albums.observe(viewLifecycleOwner) { a ->
            val items=a.find { it.name==MainActivity.currentAlbumName }?.mediaItems
            val pos=(binding.rvAlbums.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            (binding.rvAlbums.adapter as GridItemAdapter).submitList(items as List<ListItem>) {
                if (pos==0) binding.rvAlbums.scrollToPosition(0)
            }
        }
        scrollToPosition()
    }

    private fun scrollToPosition() {
        binding.rvAlbums.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                binding.rvAlbums.removeOnLayoutChangeListener(this)
                val viewAtPos = binding.rvAlbums.layoutManager!!.findViewByPosition(MainActivity.currentListPos)

                if (viewAtPos==null||!binding.rvAlbums.layoutManager!!.isViewPartiallyVisible(viewAtPos,true,true)) {
                    binding.rvAlbums.post {
                        binding.rvAlbums.layoutManager!!.scrollToPosition(MainActivity.currentListPos)
                        startPostponedEnterTransition()
                    }
                } else startPostponedEnterTransition()
            }
        })
    }

    private fun prepareTransitions() {
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z,true)
        reenterTransition=MaterialSharedAxis(MaterialSharedAxis.Z,false)
        exitTransition=Hold()
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: MutableList<String>,
                sharedElements: MutableMap<String, View>
            ) {
                val selectedVH = binding.rvAlbums.findViewHolderForAdapterPosition(MainActivity.currentListPos)
                sharedElements[names[0]] = (selectedVH as GridItemAdapter.MediaItemHolder).binding.image
            }
        })
    }

    private fun setupSystemBars(){
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode==Configuration.UI_MODE_NIGHT_NO||nightMode==Configuration.UI_MODE_NIGHT_UNDEFINED){
            WindowInsetsControllerCompat(requireActivity().window,binding.root).let {
                it.isAppearanceLightStatusBars=true
                it.isAppearanceLightNavigationBars=true
            }
        }
    }



}