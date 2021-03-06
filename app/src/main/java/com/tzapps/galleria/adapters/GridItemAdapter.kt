package com.tzapps.galleria.adapters

/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.google.android.material.shape.ShapeAppearanceModel
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.R
import com.tzapps.galleria.databinding.LayoutHeaderBinding
import com.tzapps.galleria.databinding.LayoutItemMediaBinding
import com.tzapps.galleria.fragments.AlbumDetailFragment
import com.tzapps.galleria.fragments.BottomNavFragment
import com.tzapps.galleria.models.ListItem
import com.tzapps.videoplayer.PlayerActivity
import com.tzapps.videoplayer.db.SingleVideo
import com.tzapps.videoplayer.db.VideoSource
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fragment for displaying a grid of images.
 */
class GridItemAdapter(val frag: Fragment, val isAlbum: Boolean): ListAdapter<ListItem, ViewHolder>(ListItem.ListItemDiffCallback()) {
    val enterTransitionStarted: AtomicBoolean = AtomicBoolean()
    lateinit var tracker: SelectionTracker<Long>

    companion object {
        const val ITEM_VIEW_TYPE_HEADER = 8123
    }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE -> {
                MediaItemHolder(
                    LayoutItemMediaBinding.inflate(LayoutInflater.from(parent.context),
                        parent, false), viewType)
            }
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO -> {
                MediaItemHolder(
                    LayoutItemMediaBinding.inflate(LayoutInflater.from(parent.context),
                        parent, false), viewType)

            }
            ITEM_VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    LayoutHeaderBinding.inflate(LayoutInflater.from(parent.context), parent,
                        false)
                )
            }
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is MediaItemHolder) {
            holder.onBind(position)
        } else if (holder is HeaderViewHolder) {
            holder.onBind()
        }
    }


    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.MediaItem -> (getItem(position) as ListItem.MediaItem).type
            is ListItem.Header -> ITEM_VIEW_TYPE_HEADER
            else -> 0
        }
    }

    inner class MediaItemHolder(val binding: LayoutItemMediaBinding, val type: Int): RecyclerView.ViewHolder(binding.root) {

        fun getItemDetails() : ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    itemId

            }

        fun onBind(position: Int) {
            binding.image.isActivated = tracker.isSelected(itemId)
            val obj = getItem(position) as ListItem.MediaItem
            if (binding.image.isActivated) {
                binding.image.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(70f)
            } else {
                binding.image.shapeAppearanceModel = ShapeAppearanceModel().withCornerSize(0f)
            }

            binding.image.transitionName = obj.id.toString()
            Glide.with(frag.requireActivity()).
            load(obj.uri)
                .signature(MediaStoreSignature("", obj.dateModified, 0))
                .thumbnail(0.2f)
                .listener(object : RequestListener<Drawable?> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any,
                        target: Target<Drawable?>, isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPos != position) {
                            return true
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return true
                        }
                        frag.startPostponedEnterTransition()
                        return true
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any,
                        target: Target<Drawable?>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (MainActivity.currentListPos != position) {
                            return false
                        }
                        if (enterTransitionStarted.getAndSet(true)) {
                            return false
                        }
                        frag.startPostponedEnterTransition()
                        return false
                    }
                })
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(binding.image)

            binding.image.setOnClickListener {
                //  println("layout: $layoutPosition itemList: ${(getItem(layoutPosition) as ListItem.MediaItem).listPosition} " +
                //        "itemView: ${(getItem(layoutPosition) as ListItem.MediaItem).viewPagerPosition}")

                MainActivity.currentListPos = layoutPosition
                MainActivity.currentPagerPos = if (isAlbum){
                    layoutPosition
                } else {
                    obj.pagerPosition
                }

                if (obj.type==MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                    frag.startActivity(Intent(frag.requireContext(),PlayerActivity::class.java).apply {
                        //putParcelableArrayListExtra("videoSource",VideoSource(listOf(VideoSource.SingleVideo())))
                        putExtra("videoSource",SingleVideo(obj.uri))
                    })
                }

                val extras = FragmentNavigatorExtras(it to it.transitionName)

                if (frag is BottomNavFragment) {
                    BottomNavFragment.enteringFromAlbum = false
                    val opts = NavOptions.Builder()
                    opts.setEnterAnim(android.R.anim.slide_in_left).setExitAnim(android.R.anim.slide_out_right)
                    frag.prepareTransitions()
                    frag.findNavController().navigate(
                        R.id.action_bottomNavFrag_to_viewPagerFrag,
                        null, // Bundle of args
                        opts.build(), // NavOptions
                        extras)
                } else if (frag is AlbumDetailFragment) {
                    val args = Bundle()
                    args.putBoolean("isAlbum", true)
                    try {
                        frag.findNavController().navigate(
                            R.id.action_albumDetailFrag_to_viewPagerFrag,
                            args, // Bundle of args
                            null, // NavOptions
                            extras)
                    } catch (e: java.lang.IllegalArgumentException) {
                        // tapping twice on an image
                    }

                }
            }
        }
    }

    inner class HeaderViewHolder (private val binding: LayoutHeaderBinding): RecyclerView.ViewHolder(binding.root) {
        fun onBind() {
            binding.tvDate.text = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG).format(
                Date(getItem(layoutPosition).id)
            )
        }
        fun getItemDetails() : ItemDetailsLookup.ItemDetails<Long> =
            object : ItemDetailsLookup.ItemDetails<Long>() {
                override fun getPosition(): Int =
                    layoutPosition

                override fun getSelectionKey(): Long =
                    //  (getItem(layoutPosition) as ListItem.Header).date.toString().toUri()
                    itemId
            }
    }

    override fun getItemId(position: Int): Long =
        getItem(position).id
}