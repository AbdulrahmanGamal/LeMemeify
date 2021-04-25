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

package com.raywenderlich.android.lememeify.ui.details

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.raywenderlich.android.lememeify.FileOperations
import com.raywenderlich.android.lememeify.getImageFormat
import com.raywenderlich.android.lememeify.model.Media
import com.raywenderlich.android.lememeify.ui.ImageDetailAction
import com.raywenderlich.android.lememeify.ui.ModificationType
import kotlinx.coroutines.launch

class DetailsViewModel(application: Application) : AndroidViewModel(application) {

  private val _actions = MutableLiveData<ImageDetailAction>()
  val actions: LiveData<ImageDetailAction> get() = _actions

  fun saveImage(image: Media, uri: Uri?, bitmap: Bitmap) {
    viewModelScope.launch {
      val type = getApplication<Application>().contentResolver.getType(image.uri)
      val format = getImageFormat(type!!)

      if (uri == null) {
        FileOperations.saveImage(getApplication(), bitmap, format)
      } else {
        FileOperations.saveImage(getApplication(), uri, bitmap, format)
      }

      _actions.postValue(ImageDetailAction.ImageSaved)
    }
  }

  fun updateImage(image: Media, bitmap: Bitmap) {
    viewModelScope.launch {
      val type = getApplication<Application>().contentResolver.getType(image.uri)
      val format = getImageFormat(type!!)

      val intentSender = FileOperations.updateImage(
          getApplication(), image.uri, bitmap, format)

      if (intentSender == null) {
        _actions.postValue(ImageDetailAction.ImageUpdated)
      } else {
        _actions.postValue(
            ImageDetailAction.ScopedPermissionRequired(
                intentSender,
                ModificationType.UPDATE
            )
        )
      }
    }
  }

  fun deleteImage(image: Media) {
    viewModelScope.launch {
      val intentSender = FileOperations.deleteMedia(getApplication(), image)
      if (intentSender == null) {
        _actions.postValue(ImageDetailAction.ImageDeleted)
      } else {
        _actions.postValue(
            ImageDetailAction.ScopedPermissionRequired(
                intentSender,
                ModificationType.DELETE
            )
        )
      }
    }
  }
}