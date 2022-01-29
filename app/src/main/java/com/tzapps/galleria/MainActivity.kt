package com.tzapps.galleria

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.tzapps.galleria.databinding.ActivityMainBinding
import com.tzapps.galleria.vm.GalleryViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: GalleryViewModel by viewModels()

    companion object {
        var currentListPos=0
        var currentPagerPos=0
        lateinit var currentAlbumName: String
        var isMoreOpened = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        val nightMode=resources.configuration.uiMode
        if ((nightMode==Configuration.UI_MODE_NIGHT_NO||nightMode==Configuration.UI_MODE_NIGHT_UNDEFINED)||AppCompatDelegate.getDefaultNightMode()==AppCompatDelegate.MODE_NIGHT_NO) {
            window.statusBarColor=ContextCompat.getColor(this,R.color.white)
        } else {
           window.statusBarColor=ContextCompat.getColor(this,R.color.black)
        }
        val request = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode== RESULT_OK){
                viewModel.deletePendingImage()
                viewModel.loadItems()
            }
        }
        viewModel.deletePermission.observe(this){
            val senderRequest = IntentSenderRequest.Builder(it).build()
            request.launch(senderRequest)
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadItems()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            102-> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadItems()
                } else {
                    // If we weren't granted the permission, check to see if we should show
                    // rationale for the permission.
                    val showRationale =
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        )

                    if (showRationale) {

                        Toast.makeText(this, "App requires access to storage to access your Photos", Toast.LENGTH_SHORT).show()
                    } else {
                        goToSettings()
                    }
                }
                return
            }
        }
    }

    private fun goToSettings() {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.also { intent ->
            startActivity(intent)
        }

    }


}