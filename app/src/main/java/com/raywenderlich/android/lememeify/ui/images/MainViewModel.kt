/*
 * Copyright (c) 2020 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.lememeify.ui.images

import android.app.Application
import android.database.ContentObserver
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raywenderlich.android.lememeify.FileOperations
import com.raywenderlich.android.lememeify.hasSdkHigherThan
import com.raywenderlich.android.lememeify.model.Media
import com.raywenderlich.android.lememeify.registerObserver
import com.raywenderlich.android.lememeify.ui.MainAction
import com.raywenderlich.android.lememeify.ui.ModificationType
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _actions = MutableLiveData<MainAction>()
    val actions: LiveData<MainAction> get() = _actions

    private var contentObserver: ContentObserver? = null

    fun loadImages() {
        viewModelScope.launch {
            val imageList = FileOperations.queryImagesOnDevice(getApplication<Application>())
            _actions.postValue(MainAction.ImagesChanged(imageList))

            if (contentObserver == null) {
                contentObserver = getApplication<Application>().contentResolver.registerObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI) {
                    loadImages()
                }
            }
        }
    }

    fun loadVideos() {
        viewModelScope.launch {
            val videosList = FileOperations.queryVideosOnDevice(getApplication<Application>())
            _actions.postValue(MainAction.VideosChanged(videosList))
        }
    }

    fun requestStoragePermissions() {
        _actions.postValue(MainAction.StoragePermissionsRequested)
    }

    fun deleteMedia(media: List<Media>) {
        if (hasSdkHigherThan(Build.VERSION_CODES.Q) && media.size > 1) {
            val intentSender = FileOperations.deleteMediaBulk(getApplication<Application>(), media)
            _actions.postValue(
                    MainAction.ScopedPermissionRequired(
                            intentSender,
                            ModificationType.DELETE))
        } else {
            viewModelScope.launch {
                for (item in media) {
                    val intentSender = FileOperations.deleteMedia(
                            getApplication<Application>(),
                            item)
                    if (intentSender != null) {
                        _actions.postValue(
                                MainAction.ScopedPermissionRequired(
                                        intentSender,
                                        ModificationType.DELETE))
                    }
                }
            }
        }
    }
    fun requestFavoriteMedia(media: List<Media>, state: Boolean) {
        val intentSender = FileOperations.addToFavorites(
                getApplication<Application>(),
                media,
                state)
        _actions.postValue(
                MainAction.ScopedPermissionRequired(
                        intentSender,
                        ModificationType.FAVORITE))
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun loadFavorites() {
        viewModelScope.launch {
            val mediaList = FileOperations.queryFavoriteMedia(
                    getApplication<Application>())
            _actions.postValue(MainAction.FavoriteChanged(mediaList))
        }
    }

    fun requestTrashMedia(media: List<Media>, state: Boolean) {
        val intentSender = FileOperations.addToTrash(
                getApplication<Application>(),
                media,
                state)
        _actions.postValue(MainAction.ScopedPermissionRequired(
                intentSender,
                ModificationType.TRASH))
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun loadTrashed() {
        viewModelScope.launch {
            val mediaList = FileOperations.queryTrashedMedia(
                    getApplication<Application>())
            _actions.postValue(MainAction.TrashedChanged(mediaList))
        }
    }
}