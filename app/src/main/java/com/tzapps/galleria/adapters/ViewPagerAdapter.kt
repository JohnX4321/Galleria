package com.tzapps.galleria.adapters


import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.databinding.LayoutItemImageBinding
import com.tzapps.galleria.fragments.ImageViewerFragment
import com.tzapps.galleria.models.ListItem
import java.util.concurrent.atomic.AtomicBoolean

class ViewPagerAdapter(val fragment: ImageViewerFragment): ListAdapter<ListItem.MediaItem, ViewPagerAdapter.ViewHolder>(ListItem.MediaItem.DiffCallback) {

    val transitionStarted: AtomicBoolean = AtomicBoolean()

    override fun onCreateViewHolder(parent: ViewGroup,viewType: Int) = ViewHolder(LayoutItemImageBinding.inflate(
        LayoutInflater.from(parent.context),parent,false))


    override fun onBindViewHolder(holder: ViewHolder,pos: Int) {
        holder.onBind()
    }

    inner class ViewHolder(val binding: LayoutItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            binding.pagerImage.transitionName=getItem(layoutPosition).id.toString()
            /*binding.pagerImage.enableZooming()
            binding.pagerImage.gFrag=fragment*/
            Glide.with(fragment).load(getItem(layoutPosition).uri)
                .signature(MediaStoreSignature(null,getItem(layoutPosition).dateModified,0))
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentPagerPos!=layoutPosition) return true
                        if (transitionStarted.getAndSet(true)) return true
                        fragment.startPostponedEnterTransition()
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentPagerPos!=layoutPosition) return false
                        if (transitionStarted.getAndSet(true)) return false
                        fragment.startPostponedEnterTransition()
                        return false
                    }
                }).into(binding.pagerImage)
            binding.pagerImage.setOnClickListener {
                fragment.toggleSystemUI()
            }
            binding.root.setOnClickListener {
                fragment.toggleSystemUI()
            }
        }

    }

}