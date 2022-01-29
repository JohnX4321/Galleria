package com.tzapps.galleria.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.core.view.ViewGroupCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StableIdKeyProvider
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.adapters.GridItemAdapter
import com.tzapps.galleria.databinding.LayoutItemGridBinding
import com.tzapps.galleria.models.ListItem
import com.tzapps.galleria.utils.CustomItemDetailsLookup
import com.tzapps.galleria.vm.GalleryViewModel

class GridItemFragment: Fragment() {

    companion object {
        var spanCount = 4
    }

    private lateinit var _binding: LayoutItemGridBinding
    val binding get() = _binding
    private val viewModel: GalleryViewModel by activityViewModels()
    var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!::_binding.isInitialized)
            _binding= LayoutItemGridBinding.inflate(inflater,container,false)
        viewModel.recyclerViewItems.observe(viewLifecycleOwner){i->
            val pos = (binding.rvItems.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition()
            (binding.root.adapter as GridItemAdapter).submitList(i){
                if (pos==0) binding.rvItems.scrollToPosition(0)
            }
        }
        val adapter = GridItemAdapter(requireParentFragment(),false)

        binding.root.apply {
            this.adapter=adapter
            val manager = GridLayoutManager(requireContext(), spanCount)
            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when(adapter.getItemViewType(position)){
                        GridItemAdapter.ITEM_VIEW_TYPE_HEADER-> spanCount
                        else -> 1
                    }
                }
            }
            layoutManager=manager
            setHasFixedSize(true)
        }
        val tracker=SelectionTracker.Builder("GridItemFragmentSelectionId",binding.rvItems,StableIdKeyProvider(binding.rvItems),CustomItemDetailsLookup(binding.rvItems),
            StorageStrategy.createLongStorage()).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {
            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean {
                return viewModel.recyclerViewItems.value?.contains(ListItem.Header(key))==false
            }

            override fun canSelectMultiple(): Boolean {
                return true
            }

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean {
                return true
            }
            }).build()
        adapter.tracker=tracker
        scrollToPosition()
        ViewGroupCompat.setTransitionGroup(binding.rvItems,true)
        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
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
            }
        }
        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()
                actionMode?.title = tracker.selection.size().toString()
                if (actionMode==null){
                    actionMode=(parentFragment as BottomNavFragment).startActionMode(callback = callback)
                } else if (tracker.selection.size()==0){
                    actionMode?.finish()
                    actionMode=null
                    requireActivity().window.statusBarColor=resources.getColor(android.R.color.transparent,requireActivity().theme)
                }
            }

            @SuppressLint("RestrictedApi")
            override fun onSelectionCleared() {
                actionMode=null
                requireActivity().window.statusBarColor=resources.getColor(android.R.color.transparent,requireActivity().theme)
            }
        })
        exitTransition=MaterialFadeThrough()
        enterTransition=MaterialFadeThrough()
        return binding.root
    }

    private fun scrollToPosition(){
        binding.root.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
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
                binding.rvItems.removeOnLayoutChangeListener(this)
                val viewAtPos = binding.rvItems.layoutManager!!.findViewByPosition(MainActivity.currentListPos)
                if (viewAtPos==null||!binding.rvItems.layoutManager!!.isViewPartiallyVisible(viewAtPos,true,true))
                    binding.rvItems.post { binding.rvItems.layoutManager!!.scrollToPosition(MainActivity.currentListPos) }
            }
        })
    }

}