package com.tzapps.galleria.models

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

sealed class ListItem {

    abstract val id: Long

    data class MediaItem(override val id: Long, val uri: Uri, val album: String,val type: Int,val dateModified: Long, val pagerPosition: Int, val listPosition: Int): ListItem() {
        companion object {
            val DiffCallback = object : DiffUtil.ItemCallback<MediaItem>() {
                override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                    return oldItem==newItem
                }

                override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean {
                    return oldItem.id==newItem.id
                }
            }
        }
    }

    data class Header(override val id: Long): ListItem()

    class ListItemDiffCallback: DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem.id==newItem.id
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return if (oldItem is ListItem.MediaItem && newItem is ListItem.MediaItem) oldItem.uri==newItem.uri else oldItem==newItem
        }
    }

}
