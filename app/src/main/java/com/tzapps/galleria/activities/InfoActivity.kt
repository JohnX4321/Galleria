package com.tzapps.galleria.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.aboutlibraries.LibsBuilder
import com.tzapps.galleria.databinding.ActivityInfoBinding

class InfoActivity: AppCompatActivity() {

    private lateinit var binding: ActivityInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ospBtn.setOnClickListener {
            LibsBuilder().start(this)
        }
    }

}