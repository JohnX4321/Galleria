package com.tzapps.galleria.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.tzapps.galleria.adapters.GridAlbumAdapter
import com.tzapps.galleria.databinding.LayoutAlbumGridBinding
import com.tzapps.galleria.databinding.LayoutFragmentAlbumBinding
import com.tzapps.galleria.vm.GalleryViewModel

class GridAlbumFragment: Fragment() {

    private lateinit var _binding: LayoutAlbumGridBinding
    val binding get() = _binding
    private val viewModel: GalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized) return binding.root
        _binding= LayoutAlbumGridBinding.inflate(inflater,container,false)
        val adapter = GridAlbumAdapter(requireParentFragment() as BottomNavFragment)
        binding.rvAlbum.apply {
            this.adapter=adapter
            setHasFixedSize(true)
        }
        adapter.submitList(viewModel.albums.value)
        viewModel.albums.observe(viewLifecycleOwner) {i->
            val pos=(binding.rvAlbum.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            adapter.submitList(i) {if (pos==0) binding.rvAlbum.scrollToPosition(0)}
        }
        ViewGroupCompat.setTransitionGroup(binding.rvAlbum,true)
        enterTransition=MaterialFadeThrough()
        exitTransition=MaterialFadeThrough()
        requireParentFragment().startPostponedEnterTransition()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val pos = (binding.rvAlbum.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
        (binding.rvAlbum.adapter as GridAlbumAdapter).submitList(viewModel.albums.value){
            if (pos==0) binding.rvAlbum.scrollToPosition(0)
        }
    }

    companion object {
        val spanCount = 2
    }

}