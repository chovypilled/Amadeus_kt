package com.example.chovypilled.amadeus_kt

import android.content.SharedPreferences
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

public class SettingsFragment : PreferenceFragmentCompat(){
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, null)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

}