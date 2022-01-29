package com.tzapps.galleria.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.tzapps.galleria.R
import com.tzapps.galleria.utils.Prefs

class SettingsFragment: PreferenceFragmentCompat() {

    private lateinit var trashPreference: SwitchPreference
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preference_settings,null)
        trashPreference = findPreference("deletePref")!!
        trashPreference.isChecked = Prefs.isTrashEnabled()
        trashPreference.setOnPreferenceChangeListener { _, _ ->
            Prefs.setTrashEnabled(trashPreference.isChecked)
            true
        }
    }


}