package com.tzapps.galleria.vm

import android.app.Application
import android.content.ContentUris
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tzapps.galleria.models.Album
import com.tzapps.galleria.models.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class GalleryViewModel(application: Application): AndroidViewModel(application) {

    private val _recyclerViewItems = MutableLiveData<List<ListItem>>()
    val recyclerViewItems: LiveData<List<ListItem>> get() = _recyclerViewItems

    private val _viewPagerItems = MutableLiveData<List<ListItem.MediaItem>>()
    val viewPagerItems: LiveData<List<ListItem.MediaItem>> get() = _viewPagerItems

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> get() = _albums

    private var pendingDelete: ListItem.MediaItem? = null
    private val _deletePermission = MutableLiveData<IntentSender>()
    val deletePermission: LiveData<IntentSender> = _deletePermission

    fun loadItems() {
        viewModelScope.launch {
            val imgList = queryimages()
            val viewPagerImageList = extractItems(imgList)
            _viewPagerItems.postValue(viewPagerImageList)
            _recyclerViewItems.postValue(imgList)
            _albums.postValue(getAlbums(viewPagerImageList))
        }
    }

    suspend fun postImages(images: List<ListItem>) {
        viewModelScope.launch {
            _recyclerViewItems.postValue(images)
        }
    }

    fun deleteExistingImage(image: ListItem.MediaItem?) {
        if (image==null) return
        viewModelScope.launch {
            deleteImage(image)
        }
    }

    fun deletePendingImage(){
        pendingDelete?.let {
            pendingDelete=null
            deleteExistingImage(it)
        }
    }

    private suspend fun extractItems(items: List<ListItem>): List<ListItem.MediaItem> {
        val vpImages = mutableListOf<ListItem.MediaItem>()
        for (i in items)
            if (i is ListItem.MediaItem) vpImages+=i
        return vpImages
    }

    private suspend fun queryimages(): List<ListItem>{

        val images = mutableListOf<ListItem>()
        var listPos = -1
        var viewPagerPos = -1

        withContext(Dispatchers.IO) {
            val proj = arrayOf(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,MediaStore.Files.FileColumns._ID,MediaStore.Files.FileColumns.MEDIA_TYPE,MediaStore.Files.FileColumns.DATE_ADDED,MediaStore.Files.FileColumns.DATE_MODIFIED,MediaStore.Files.FileColumns.DISPLAY_NAME)
            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE+"="+MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE+" OR "+MediaStore.Files.FileColumns.MEDIA_TYPE+"="+MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            getApplication<Application>().contentResolver.query(
                contentUri,proj,selection,null,sortOrder
            )?.use { c->
                val typeC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val idC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dateAddedC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val bucketNameC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val dateModifiedC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                var lastDate: Date? = null
                while (c.moveToNext()) {
                    val type = c.getInt(typeC)
                    val id = c.getLong(idC)
                    var dateAdded = c.getLong(dateAddedC)
                    val dateModified = c.getLong(dateModifiedC)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000
                    val album = c.getString(bucketNameC)?: ""
                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    val selectedDate = Date(dateAdded)
                    val sdc=Calendar.getInstance().apply { time=selectedDate }
                    val ldc=if (lastDate!=null) Calendar.getInstance().apply { time=lastDate!! } else null
                    if (lastDate == null || ldc!!.get(Calendar.DATE) > sdc.get(Calendar.DATE) || ldc.get(Calendar.MONTH) > sdc.get(Calendar.MONTH)
                        || ldc.get(Calendar.YEAR) > sdc.get(Calendar.YEAR))  {
                        images += ListItem.Header(dateAdded)
                        lastDate = selectedDate
                        listPos += 1
                    }
                    viewPagerPos += 1
                    listPos += 1
                    images += ListItem.MediaItem( id, uri, album, type, dateModified, viewPagerPos, listPos)
                }
            }

        }
        return images
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private suspend fun queryTrashMedia(): List<ListItem> {
        val media = mutableListOf<ListItem>()
        var listPos = -1
        var viewPagerPos = -1

        withContext(Dispatchers.IO) {
            val proj = arrayOf(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,MediaStore.Files.FileColumns._ID,MediaStore.Files.FileColumns.MEDIA_TYPE,MediaStore.Files.FileColumns.DATE_ADDED,MediaStore.Files.FileColumns.DATE_MODIFIED,MediaStore.Files.FileColumns.DISPLAY_NAME)
            val selection = (MediaStore.Files.FileColumns.IS_TRASHED+"=TRUE")
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
            val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            getApplication<Application>().contentResolver.query(
                contentUri,proj,selection,null,sortOrder
            )?.use { c->
                val typeC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val idC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dateAddedC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val bucketNameC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val dateModifiedC=c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                var lastDate: Date? = null
                while (c.moveToNext()) {
                    val type = c.getInt(typeC)
                    val id = c.getLong(idC)
                    var dateAdded = c.getLong(dateAddedC)
                    val dateModified = c.getLong(dateModifiedC)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000
                    val album = c.getString(bucketNameC)?: ""
                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    val selectedDate = Date(dateAdded)
                    val sdc=Calendar.getInstance().apply { time=selectedDate }
                    val ldc=if (lastDate!=null) Calendar.getInstance().apply { time=lastDate!! } else null
                    if (lastDate == null || ldc!!.get(Calendar.DATE) > sdc.get(Calendar.DATE) || ldc.get(Calendar.MONTH) > sdc.get(Calendar.MONTH)
                        || ldc.get(Calendar.YEAR) > sdc.get(Calendar.YEAR))  {
                        media += ListItem.Header(dateAdded)
                        lastDate = selectedDate
                        listPos += 1
                    }
                    viewPagerPos += 1
                    listPos += 1
                    media += ListItem.MediaItem( id, uri, album, type, dateModified, viewPagerPos, listPos)
                }
            }

        }
        return media
    }

    private suspend fun getAlbums(mediaList: List<ListItem.MediaItem>?): List<Album> {
        val albums= mutableListOf<Album>()

        mediaList?: return albums
        withContext(Dispatchers.Main) {
            albums+=(Album("null", mutableListOf()))
            for (item in mediaList) {
                for (i in albums.indices) {
                    if (albums[i].name==item.album) {
                        albums[i].mediaItems+=item
                        break
                    } else if (i==albums.lastIndex) {
                        albums+=Album(item.album, mutableListOf())
                        albums[i+1].mediaItems+=item
                    }
                }
            }
            albums.removeAt(0)
        }
        return albums
    }

    private suspend fun deleteImage(image: ListItem.MediaItem) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createTrashRequest(getApplication<Application>().contentResolver,
                        listOf(image.uri),true)
                    pendingDelete=null
                    _deletePermission.postValue(pendingIntent.intentSender)
                } else {
                    getApplication<Application>().contentResolver.delete(image.uri,"${MediaStore.Files.FileColumns._ID}=?",
                        arrayOf(image.id.toString()))
                }
            } catch (e: Exception) {
                pendingDelete=image
                
            }
        }
    }

}