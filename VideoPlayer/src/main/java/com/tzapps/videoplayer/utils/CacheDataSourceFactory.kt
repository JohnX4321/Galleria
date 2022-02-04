package com.tzapps.videoplayer.utils

import android.content.Context
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.exoplayer2.util.Util
import com.tzapps.videoplayer.R
import java.io.File

class CacheDataSourceFactory(val context: Context,val maxCacheSize: Long, val maxFileSize: Long): DataSource.Factory {

    companion object {
        private var simpleCache: SimpleCache? = null
    }
    private var defaultDataSourceFactory: DataSource.Factory = DefaultDataSource.Factory(context)

    override fun createDataSource(): DataSource {
        val evictor = LeastRecentlyUsedCacheEvictor(maxCacheSize)
        simpleCache= getInstance(evictor)
        return CacheDataSource(simpleCache!!,defaultDataSourceFactory.createDataSource(),FileDataSource(),CacheDataSink(
            simpleCache!!,maxFileSize),CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,null)
    }

    fun getInstance(evictor: LeastRecentlyUsedCacheEvictor): SimpleCache {
        if (simpleCache==null) simpleCache= SimpleCache(File(context.cacheDir,"media"),evictor)
        return simpleCache!!
    }

}