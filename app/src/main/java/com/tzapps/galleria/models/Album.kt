package com.tzapps.galleria.models

import androidx.recyclerview.widget.DiffUtil

data class Album(var name: String, var mediaItems: MutableList<ListItem.MediaItem>) {
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Album>() {
            override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
                return oldItem.mediaItems==newItem.mediaItems
            }

            override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
                return oldItem.name==newItem.name
            }
        }
    }
}
