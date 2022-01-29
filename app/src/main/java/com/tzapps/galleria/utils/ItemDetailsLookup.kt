package com.tzapps.galleria.utils

import android.view.MotionEvent
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.widget.RecyclerView
import com.tzapps.galleria.adapters.GridItemAdapter

class CustomItemDetailsLookup(private val recyclerView: RecyclerView): ItemDetailsLookup<Long>() {

    override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
        val view = recyclerView.findChildViewUnder(e.x,e.y)?: return null
        val viewHolder = recyclerView.getChildViewHolder(view)?: return null
        return when(viewHolder) {
            is GridItemAdapter.MediaItemHolder -> viewHolder.getItemDetails()
            is GridItemAdapter.HeaderViewHolder-> viewHolder.getItemDetails()
            else-> null
        }
    }

}