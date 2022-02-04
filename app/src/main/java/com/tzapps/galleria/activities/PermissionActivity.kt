package com.tzapps.galleria.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tzapps.galleria.MainActivity
import com.tzapps.galleria.databinding.ActivityPermissionsBinding

class PermissionActivity: AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private val permList = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.grantPermissionsBtn.setOnClickListener {
            reqPermissions()
        }
    }

    private val managerStorageResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r->
        if (r.resultCode==Activity.RESULT_OK) {
            if (haveStoragePerm()) {
                startMainActivity()
            }
            else Toast.makeText(this,"Required Permission denied",Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (haveStoragePerm()) {
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==102) {
            if (grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                startMainActivity()
            }
        }
    }


    private fun haveStoragePerm() =if (Build.VERSION.SDK_INT< Build.VERSION_CODES.R) ContextCompat.checkSelfPermission(this,
        Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED else Environment.isExternalStorageManager()

    private fun reqPermissions() {
        if (Build.VERSION.SDK_INT< Build.VERSION_CODES.R) {
            ActivityCompat.requestPermissions(this,permList , 102)
        } else {
            managerStorageResultLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

}