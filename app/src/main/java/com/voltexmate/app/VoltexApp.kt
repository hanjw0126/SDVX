package com.voltexmate.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.voltexmate.app.data.Net

class VoltexApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { Net.client }
            .crossfade(true)
            .build()
}
