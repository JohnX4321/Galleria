package com.tzapps.galleria.camera.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<T: ViewBinding>(private val fragmentLayout: Int): Fragment() {

    abstract val binding: T

    protected val outputDirectory: String by lazy { if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q) "${Environment.DIRECTORY_DCIM}/Galleria/" else "${requireContext().getExternalFilesDir(Environment.DIRECTORY_DCIM)}/Galleria/" }

    private val perm = mutableListOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_MEDIA_LOCATION)
        }
    }

    private val permReq = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {p->
        if (p.all { it.value })
            onPermGranted()
        else {
            view?.let { v->
                Toast.makeText(requireContext(),"Permissions not granted",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressed()
            }
        })
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (hasAllPermissions()) {
            onPermGranted()
        } else permReq.launch(perm.toTypedArray())
    }

    /**
     * Check for the permissions
     */
    protected fun hasAllPermissions() = perm.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * A function which will be called after the permission check
     * */
    open fun onPermGranted() = Unit

    /**
     * An abstract function which will be called on the Back button press
     * */
    abstract fun onBackPressed()



}