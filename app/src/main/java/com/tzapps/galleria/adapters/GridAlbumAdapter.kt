package com.tzapps.galleria.adapters

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.MediaStoreSignature
import com.google.android.material.transition.MaterialSharedAxis
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.R
import com.tzapps.galleria.databinding.LayoutAlbumGridBinding
import com.tzapps.galleria.databinding.LayoutItemAlbumBinding
import com.tzapps.galleria.fragments.BottomNavFragment
import com.tzapps.galleria.models.Album

class GridAlbumAdapter(private val fragment: BottomNavFragment): ListAdapter<Album, GridAlbumAdapter.AlbumViewHolder>(Album.DiffCallback)  {

    inner class AlbumViewHolder(private val binding: LayoutItemAlbumBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            Glide.with(fragment.requireActivity())
                .load(getItem(layoutPosition).mediaItems[0].uri)
                .signature(MediaStoreSignature("",getItem(layoutPosition).mediaItems[0].dateModified,0))
                .thumbnail(0.3f).into(binding.ivThumbnailAlbum)
            binding.tvAlbumName.text=getItem(layoutPosition).name
            binding.ivThumbnailAlbum.transitionName="album_$layoutPosition"

            binding.ivThumbnailAlbum.setOnClickListener {
                try {
                    fragment.exitTransition=MaterialSharedAxis(MaterialSharedAxis.Z,true)
                    MainActivity.currentListPos=0
                    MainActivity.currentAlbumName=getItem(layoutPosition).name
                    fragment.findNavController().navigate(R.id.action_bottomNavFrag_to_albumDetailFrag,null,null,null)
                }  catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int) = AlbumViewHolder(LayoutItemAlbumBinding.inflate(
        LayoutInflater.from(parent.context),parent,false))

    override fun onBindViewHolder(holder: AlbumViewHolder,pos: Int){
        holder.onBind()
    }

}