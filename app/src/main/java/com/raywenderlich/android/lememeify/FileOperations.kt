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

package com.raywenderlich.android.lememeify

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.raywenderlich.android.lememeify.model.Media
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val QUALITY = 100

object FileOperations {

  suspend fun queryImagesOnDevice(context: Context, selection: String? = null): List<Media> {
    val images = mutableListOf<Media>()

    withContext(Dispatchers.IO) {
      var projection = arrayOf(MediaStore.Images.Media._ID,
          MediaStore.Images.Media.RELATIVE_PATH,
          MediaStore.Images.Media.DISPLAY_NAME,
          MediaStore.Images.Media.SIZE,
          MediaStore.Images.Media.MIME_TYPE,
          MediaStore.Images.Media.WIDTH,
          MediaStore.Images.Media.HEIGHT,
          MediaStore.Images.Media.DATE_MODIFIED)

      if (hasSdkHigherThan(Build.VERSION_CODES.Q)) {
        projection += arrayOf(MediaStore.Images.Media.IS_FAVORITE)
      }

      val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

      context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          null,
          sortOrder)?.use { cursor ->

        while (cursor.moveToNext()) {
          val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID))
          val path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH))
          val name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
          val size = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.SIZE))
          val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE))
          val width = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH))
          val height = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT))
          val date = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_MODIFIED))

          val favorite =
              if (hasSdkHigherThan(Build.VERSION_CODES.Q)) {
                cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.IS_FAVORITE))
              } else {
                "0"
              }

          val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

          // Discard invalid images that might exist on the device
          if (size == null) {
            continue
          }

          images += Media(id, uri, path, name, size, mimeType, width, height, date, favorite == "1")
        }

        cursor.close()
      }
    }

    return images
  }

  suspend fun queryVideosOnDevice(context: Context, selection: String? = null): List<Media> {
    val videos = mutableListOf<Media>()

    withContext(Dispatchers.IO) {
      var projection = arrayOf(MediaStore.Video.Media._ID,
          MediaStore.Video.Media.RELATIVE_PATH,
          MediaStore.Video.Media.DISPLAY_NAME,
          MediaStore.Video.Media.SIZE,
          MediaStore.Video.Media.MIME_TYPE,
          MediaStore.Video.Media.WIDTH,
          MediaStore.Video.Media.HEIGHT,
          MediaStore.Video.Media.DATE_MODIFIED)

      if (hasSdkHigherThan(Build.VERSION_CODES.Q)) {
        projection += arrayOf(MediaStore.Images.Media.IS_FAVORITE)
      }

      val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"

      context.contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          null,
          sortOrder)?.use { cursor ->

        while (cursor.moveToNext()) {
          val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Video.Media._ID))
          val path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH))
          val name = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME))
          val size = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.SIZE))
          val mimeType = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE))
          val width = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.WIDTH))
          val height = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT))
          val date = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATE_MODIFIED))

          val favorite =
              if (hasSdkHigherThan(Build.VERSION_CODES.Q)) {
                cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.IS_FAVORITE))
              } else {
                "0"
              }

          val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

          // Discard invalid images that might exist on the device
          if (size == null) {
            continue
          }

          videos += Media(id, uri, path, name, size, mimeType, width, height, date, favorite == "1")
        }

        cursor.close()
      }
    }

    return videos
  }

  suspend fun saveImage(context: Context, uri: Uri, bitmap: Bitmap, format: CompressFormat) {
    withContext(Dispatchers.IO) {
      context.contentResolver.openOutputStream(uri, "w").use {
        bitmap.compress(format, QUALITY, it)
      }
    }
  }

  suspend fun saveImage(context: Context, bitmap: Bitmap, format: CompressFormat) {
    withContext(Dispatchers.IO) {
      val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
      val dirDest = File(Environment.DIRECTORY_PICTURES, context.getString(R.string.app_name))
      val date = System.currentTimeMillis()
      val extension = getImageExtension(format)

      val newImage = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "$date.$extension")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/$extension")
        put(MediaStore.MediaColumns.DATE_ADDED, date)
        put(MediaStore.MediaColumns.DATE_MODIFIED, date)
        put(MediaStore.MediaColumns.SIZE, bitmap.byteCount)
        put(MediaStore.MediaColumns.WIDTH, bitmap.width)
        put(MediaStore.MediaColumns.HEIGHT, bitmap.height)
        put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest${File.separator}")
        put(MediaStore.Images.Media.IS_PENDING, 1)
      }

      val newImageUri = context.contentResolver.insert(collection, newImage)

      context.contentResolver.openOutputStream(newImageUri!!, "w").use {
        bitmap.compress(format, QUALITY, it)
      }

      newImage.clear()
      newImage.put(MediaStore.Images.Media.IS_PENDING, 0)
      context.contentResolver.update(newImageUri, newImage, null, null)
    }
  }

  @SuppressLint("NewApi") //method only call from API 29 onwards
  suspend fun updateImage(context: Context, uri: Uri, bitmap: Bitmap,
                          format: CompressFormat): IntentSender? {

    var result: IntentSender? = null
    withContext(Dispatchers.IO) {
      try {
        saveImage(context, uri, bitmap, format)
      } catch (securityException: SecurityException) {

        if (hasSdkHigherThan(Build.VERSION_CODES.P)) {
          val recoverableSecurityException =
              securityException as? RecoverableSecurityException ?: throw securityException

          result = recoverableSecurityException.userAction.actionIntent.intentSender
        } else {
          throw securityException
        }
      }
    }

    return result
  }

  @SuppressLint("NewApi") //method only call from API 29 onwards
  suspend fun deleteMedia(context: Context, media: Media): IntentSender? {
    var result: IntentSender? = null
    withContext(Dispatchers.IO) {
      try {
        context.contentResolver.delete(media.uri, "${MediaStore.Images.Media._ID} = ?",
            arrayOf(media.id.toString()))
      } catch (securityException: SecurityException) {
        if (hasSdkHigherThan(Build.VERSION_CODES.P)) {
          val recoverableSecurityException =
              securityException as? RecoverableSecurityException ?: throw securityException

          result = recoverableSecurityException.userAction.actionIntent.intentSender
        } else {
          throw securityException
        }
      }
    }

    return result
  }

  fun deleteMediaBulk(context: Context, media: List<Media>): IntentSender {
    val uris = media.map { it.uri }
    return MediaStore.createDeleteRequest(context.contentResolver,
            uris).intentSender
  }
  @SuppressLint("NewApi") //method only call from API 30 onwards
  fun addToFavorites(context: Context, media: List<Media>, state: Boolean): IntentSender {
    val uris = media.map { it.uri }
    return MediaStore.createFavoriteRequest(
            context.contentResolver,
            uris,
            state).intentSender
  }
  @RequiresApi(Build.VERSION_CODES.R)
  suspend fun queryFavoriteMedia(context: Context): List<Media> {
    val favorite = mutableListOf<Media>()
    withContext(Dispatchers.IO) {
      val selection = "${MediaStore.MediaColumns.IS_FAVORITE} = 1"
      favorite.addAll(queryImagesOnDevice(context, selection))
      favorite.addAll(queryVideosOnDevice(context, selection))
    }
    return favorite
  }
  @SuppressLint("NewApi") //method only call from API 30 onwards
  fun addToTrash(context: Context, media: List<Media>, state: Boolean):
          IntentSender {
    val uris = media.map { it.uri }
    return MediaStore.createTrashRequest(
            context.contentResolver,
            uris,
            state).intentSender
  }

  @RequiresApi(Build.VERSION_CODES.R)
  suspend fun queryTrashedMedia(context: Context): List<Media> {
    val trashed = mutableListOf<Media>()

    withContext(Dispatchers.IO) {
      trashed.addAll(queryTrashedMediaOnDevice(
              context,
              MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
      trashed.addAll(queryTrashedMediaOnDevice(
              context,
              MediaStore.Video.Media.EXTERNAL_CONTENT_URI))
    }
    return trashed
  }
  @RequiresApi(Build.VERSION_CODES.R)
  suspend fun queryTrashedMediaOnDevice(context: Context, contentUri: Uri): List<Media> {
    val media = mutableListOf<Media>()
    withContext(Dispatchers.IO) {
      //1
      val projection = arrayOf(MediaStore.MediaColumns._ID,
              MediaStore.MediaColumns.RELATIVE_PATH,
              MediaStore.MediaColumns.DISPLAY_NAME,
              MediaStore.MediaColumns.SIZE,
              MediaStore.MediaColumns.MIME_TYPE,
              MediaStore.MediaColumns.WIDTH,
              MediaStore.MediaColumns.HEIGHT,
              MediaStore.MediaColumns.DATE_MODIFIED,
              MediaStore.MediaColumns.IS_FAVORITE,
              MediaStore.MediaColumns.IS_TRASHED)

      //2
      val bundle = Bundle()
      bundle.putInt("android:query-arg-match-trashed", 1)
      bundle.putString("android:query-arg-sql-selection",
              "${MediaStore.MediaColumns.IS_TRASHED} = 1")
      bundle.putString("android:query-arg-sql-sort-order",
              "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")

      //3
      context.contentResolver.query(
              contentUri,
              projection,
              bundle,
              null
      )?.use { cursor ->

        //4
        while (cursor.moveToNext()) {
          val id = cursor.getLong(cursor.getColumnIndex(
                  MediaStore.MediaColumns._ID))
          val path = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.RELATIVE_PATH))
          val name = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.DISPLAY_NAME))
          val size = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.SIZE))
          val mimeType = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.MIME_TYPE))
          val width = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.WIDTH))
          val height = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.HEIGHT))
          val date = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.DATE_MODIFIED))
          val favorite = cursor.getString(cursor.getColumnIndex(
                  MediaStore.MediaColumns.IS_FAVORITE))
          val uri = ContentUris.withAppendedId(contentUri, id)
          // Discard invalid images that might exist on the device
          if (size == null) {
            continue
          }
          media += Media(id,
                  uri,
                  path,
                  name,
                  size,
                  mimeType,
                  width,
                  height,
                  date,
                  favorite == "1",
                  true)
        }
        cursor.close()
      }
    }
    return media
  }
}