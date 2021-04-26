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

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.selection.SelectionPredicates
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.raywenderlich.android.lememeify.*
import com.raywenderlich.android.lememeify.databinding.FragmentMainBinding
import com.raywenderlich.android.lememeify.ui.MainAction
import com.raywenderlich.android.lememeify.ui.ModificationType

private const val NUMBER_OF_COLUMNS = 5
private const val REQUEST_PERMISSION_MEDIA = 100
private const val REQUEST_PERMISSION_DELETE = 400
private const val REQUEST_PERMISSION_FAVORITE = 500
private const val REQUEST_PERMISSION_TRASH = 600

class MainFragment : Fragment(), ActionMode.Callback {

  private val viewModel: MainViewModel by viewModels()

  private var permissionDenied = false
  private var actionMode: ActionMode? = null

  private val imageAdapter by lazy {
    MediaAdapter { clickedMedia ->
      if (clickedMedia.mimeType.contains(Regex(MIME_TYPE_IMAGE_REGEX))) {
        navigateToDetails(requireView(), clickedMedia)
      } else {
        Snackbar.make(binding.root, R.string.not_available_edit_video, Snackbar.LENGTH_SHORT).show()
      }
    }
  }

  private lateinit var binding: FragmentMainBinding
  private lateinit var tracker: SelectionTracker<String>

  override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
    setHasOptionsMenu(true)
    viewModel.actions.observe(viewLifecycleOwner, Observer { handleAction(it) })
    binding = FragmentMainBinding.inflate(inflater)
    binding.rvImages.adapter = imageAdapter
    return binding.root
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    setupToolbar()
    setupUiComponents()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_PERMISSION_DELETE -> {
        if (resultCode == Activity.RESULT_OK) {
          val multiSelection = tracker.selection.size() > 1
          if (!multiSelection || !hasSdkHigherThan(Build.VERSION_CODES.Q)) {
            delete()
          }
        } else {
          Snackbar.make(binding.root,
              R.string.image_fail, Snackbar.LENGTH_SHORT).show()
        }

        actionMode?.finish()
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (!hasStoragePermission(requireContext())) {
      if (!permissionDenied) {
        viewModel.requestStoragePermissions()
      }
      return
    }
    viewModel.loadImages()
  }

  override fun onPause() {
    permissionDenied = false
    super.onPause()
  }

  override fun onRequestPermissionsResult(code: Int, permission: Array<out String>, res: IntArray) {
    when (code) {
      REQUEST_PERMISSION_MEDIA -> {
        when {
          res.isEmpty() -> {
            //Do nothing, app is resuming
          }
          res[0] == PackageManager.PERMISSION_GRANTED -> {
            setupUiComponents()
          }
          else -> {
            permissionDenied = true
            Snackbar.make(binding.root,
                R.string.missing_permission_media, Snackbar.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_main, menu)
  }

  @RequiresApi(Build.VERSION_CODES.R)
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.filter_images -> {
        viewModel.loadImages()
        true
      }
      R.id.filter_videos -> {
        viewModel.loadVideos()
        true
      }
      R.id.filter_favorite -> {
        loadFavorites()
        true
      }
      R.id.filter_trash -> {
        loadTrashed()
        true
      }
      else -> {
        super.onOptionsItemSelected(item)
      }
    }
  }
  @RequiresApi(Build.VERSION_CODES.R)
  private fun loadFavorites() {
    if (!hasSdkHigherThan(Build.VERSION_CODES.Q)) {
      Snackbar.make(
              binding.root,
              R.string.not_available_feature,
              Snackbar.LENGTH_SHORT).show()
      return
    }
    viewModel.loadFavorites()
  }

  private fun handleAction(action: MainAction) {
    when (action) {
      is MainAction.ImagesChanged -> {
        imageAdapter.submitList(action.images)

        if (action.images.isEmpty()) {
          Snackbar.make(binding.root, R.string.no_images_on_device, Snackbar.LENGTH_SHORT).show()
        }
      }
      is MainAction.VideosChanged -> {
        imageAdapter.submitList(action.videos)

        if (action.videos.isEmpty()) {
          Snackbar.make(binding.root, R.string.no_video_on_device, Snackbar.LENGTH_SHORT).show()
        }
      }
      is MainAction.FavoriteChanged -> {
        imageAdapter.submitList(action.favorites)
        if (action.favorites.isEmpty()) {
          Snackbar.make(binding.root, R.string.no_favorite_media,
                  Snackbar.LENGTH_SHORT).show()
        }
      }
      is MainAction.TrashedChanged -> {
        imageAdapter.submitList(action.trashed)
        if (action.trashed.isEmpty()) {
          Snackbar.make(binding.root, R.string.no_trashed_media,
                  Snackbar.LENGTH_SHORT).show()
        }
      }
      is MainAction.ScopedPermissionRequired -> {
        requestScopedPermission(action.intentSender, action.forType)
      }
      MainAction.StoragePermissionsRequested -> requestStoragePermission(this,
          REQUEST_PERMISSION_MEDIA)
    }
  }

  private fun setupToolbar() {
    val appCompatActivity = activity as AppCompatActivity
    appCompatActivity.setSupportActionBar(binding.tbMain)
    appCompatActivity.setTitle(R.string.app_header)
  }

  private fun setupUiComponents() {
    val spacing = resources.getDimensionPixelSize(R.dimen.grid_space) / 2
    binding.rvImages.apply {
      setHasFixedSize(true)
      setPadding(spacing, spacing, spacing, spacing)
      addItemDecoration(object : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(rect: Rect,
                                    view: View,
                                    parent: RecyclerView,
                                    state: RecyclerView.State) {
          rect.set(spacing, spacing, spacing, spacing)
        }
      })
      layoutManager = GridLayoutManager(requireContext(), NUMBER_OF_COLUMNS)
    }

    tracker = SelectionTracker.Builder(
        "imagesSelection",
        binding.rvImages,
        ImageKeyProvider(imageAdapter),
        ImageItemDetailsLookup(binding.rvImages),
        StorageStrategy.createStringStorage()
    ).withSelectionPredicate(
        SelectionPredicates.createSelectAnything()
    ).build()

    tracker.addObserver(
        object : SelectionTracker.SelectionObserver<Long>() {
          override fun onSelectionChanged() {
            super.onSelectionChanged()

            if (actionMode == null) {
              actionMode = activity?.startActionMode(this@MainFragment)
            }

            val items = tracker.selection!!.size()
            if (items > 0) {
              actionMode?.title = getString(R.string.action_selected, items)
            } else {
              actionMode?.finish()
            }
          }
        })

    imageAdapter.tracker = tracker
  }

  private fun requestScopedPermission(intentSender: IntentSender, requestType: ModificationType) {
    val requestCode = when (requestType) {
      ModificationType.UPDATE -> return
      ModificationType.DELETE -> REQUEST_PERMISSION_DELETE
      ModificationType.FAVORITE -> REQUEST_PERMISSION_FAVORITE
      ModificationType.TRASH -> REQUEST_PERMISSION_TRASH
    }

    startIntentSenderForResult(intentSender, requestCode, null, 0, 0,
        0, null)
  }

  //region ActionMode

  override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
    return when (item!!.itemId) {
      R.id.action_delete -> {
        delete()
        true
      }
      R.id.action_favorite -> {
        addToFavorites()
        true
      }
      R.id.action_trash -> {
        addToTrash()
        true
      }
      else -> {
        false
      }
    }
  }
  private fun addToFavorites() {
    //1
    if (!hasSdkHigherThan(Build.VERSION_CODES.Q)) {
      Snackbar.make(
              binding.root,
              R.string.not_available_feature,
              Snackbar.LENGTH_SHORT).show()
      return
    }

    //2
    val media = imageAdapter.currentList.filter {
      tracker.selection.contains("${it.id}")
    }

    //3
    val state = !(media.isNotEmpty() && media[0].favorite)
    //4
    viewModel.requestFavoriteMedia(media, state)
    //5
    actionMode?.finish()
  }
  override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    mode?.menuInflater?.inflate(R.menu.action_main, menu)
    return true
  }

  override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
    if (tracker.selection.isEmpty) {
      return true
    }

    val media = imageAdapter.currentList.first {
      tracker.selection.contains("${it.id}")
    }

    return true
  }

  override fun onDestroyActionMode(mode: ActionMode?) {
    tracker.clearSelection()
    actionMode = null
  }

  //endregion

  private fun delete() {
    val media = imageAdapter.currentList.filter {
      tracker.selection.contains("${it.id}")
    }

    viewModel.deleteMedia(media)
  }
  private fun addToTrash() {
    //1
    if (!hasSdkHigherThan(Build.VERSION_CODES.Q)) {
      Snackbar.make(
              binding.root,
              R.string.not_available_feature,
              Snackbar.LENGTH_SHORT).show()
      return
    }

    //2
    val media = imageAdapter.currentList.filter {
      tracker.selection.contains("${it.id}")
    }

    //3
    val state = !(media.isNotEmpty() && media[0].trashed)
    //4
    viewModel.requestTrashMedia(media, state)
    //5
    actionMode?.finish()
  }
  @RequiresApi(Build.VERSION_CODES.R)
  private fun loadTrashed() {
    if (!hasSdkHigherThan(Build.VERSION_CODES.Q)) {
      Snackbar.make(
              binding.root,
              R.string.not_available_feature,
              Snackbar.LENGTH_SHORT).show()
      return
    }

    viewModel.loadTrashed()
  }
}