package com.example.rma

import android.app.Application
import android.util.Log
import com.cloudinary.android.MediaManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = HashMap<String,String>()
        config["cloud_name"] = BuildConfig.CLOUD_NAME
        Log.e("API KEY","CLOUD_NAME: ${BuildConfig.CLOUD_NAME}")
        config["api_key"] = BuildConfig.CLOUD_API_KEY
        config["api_secret"] = BuildConfig.CLOUD_API_SECRET

        MediaManager.init(this, config)
    }
}
