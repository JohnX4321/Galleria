package com.tzapps.galleria.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tzapps.galleria.databinding.ActivityCameraBinding

class CameraActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED||ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this,"Please Grant permissions to proceed",Toast.LENGTH_SHORT).show()
            startActivity(Intent(this,PermissionActivity::class.java))
            finish()
        }

    }

}