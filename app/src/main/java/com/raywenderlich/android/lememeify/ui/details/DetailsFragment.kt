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

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.google.android.material.snackbar.Snackbar
import com.raywenderlich.android.lememeify.*
import com.raywenderlich.android.lememeify.databinding.FragmentDetailsBinding
import com.raywenderlich.android.lememeify.model.Media
import com.raywenderlich.android.lememeify.ui.ImageDetailAction
import com.raywenderlich.android.lememeify.ui.ModificationType

private const val KEYBOARD_HIDDEN_DELAY = 250L
private const val REQUEST_PERMISSION_DELETE = 100
private const val REQUEST_PERMISSION_UPDATE = 200
private const val REQUEST_PERMISSION_MEDIA_ACCESS = 300
private const val REQUEST_SAVE_AS = 400

class DetailsFragment : Fragment() {

  private val viewModel: DetailsViewModel by viewModels()

  private lateinit var image: Media
  private lateinit var binding: FragmentDetailsBinding

  override fun onCreateView(inflater: LayoutInflater, group: ViewGroup?, state: Bundle?): View? {
    setHasOptionsMenu(true)
    viewModel.actions.observe(viewLifecycleOwner, Observer { handleAction(it) })
    binding = FragmentDetailsBinding.inflate(inflater)
    return binding.root
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    setupToolbar()

    binding.tbDetails.setNavigationOnClickListener {
      hideKeyboard {
        navigateToMain(requireView())
      }
    }

    image = requireArguments().get(EXTRA_IMAGE) as Media
    Glide.with(requireContext())
        .load(image.uri)
        .signature(ObjectKey(image.date))
        .into(binding.ivImage)

    binding.ivMeme.setOnClickListener {
      showHideMemeBuilder()
    }

    binding.ivInfo.setOnClickListener {
      hideKeyboard { showHideInfoImage() }
    }

    binding.ivDelete.setOnClickListener {
      hideKeyboard { viewModel.deleteImage(image) }
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_SAVE_AS -> {
        if (resultCode == Activity.RESULT_OK) {
          saveMeme(data?.data)
        }
      }
      REQUEST_PERMISSION_UPDATE -> {
        if (resultCode == Activity.RESULT_OK) {
          updateMeme()
        } else {
          Snackbar.make(binding.root,
              R.string.image_fail, Snackbar.LENGTH_SHORT).show()
        }
      }
      REQUEST_PERMISSION_DELETE -> {
        if (resultCode == Activity.RESULT_OK) {
          viewModel.deleteImage(image)
        } else {
          Snackbar.make(binding.root,
              R.string.image_fail, Snackbar.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onRequestPermissionsResult(code: Int, permission: Array<out String>,
                                          result: IntArray) {
    super.onRequestPermissionsResult(code, permission, result)
    when (code) {
      REQUEST_PERMISSION_MEDIA_ACCESS -> {
        if (result[0] == PackageManager.PERMISSION_GRANTED) {
          showHideInfoImage()
        } else {
          Snackbar.make(binding.root,
              R.string.missing_permission_media, Snackbar.LENGTH_SHORT).show()
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.menu_details, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_save -> {
        updateMeme()
        true
      }
      R.id.action_save_copy -> {
        saveMeme(null)
        true
      }
      R.id.action_save_location -> {
        hideKeyboard(null)
        saveMemeAs()
        true
      }
      else -> {
        super.onOptionsItemSelected(item)
      }
    }
  }

  private fun handleAction(action: ImageDetailAction) =
      when (action) {
        ImageDetailAction.ImageDeleted -> imageChanged(getString(R.string.deleted_all))
        ImageDetailAction.ImageSaved -> imageChanged(getString(R.string.image_saved))
        ImageDetailAction.ImageUpdated -> imageChanged(getString(R.string.image_updated))
        is ImageDetailAction.ScopedPermissionRequired ->
          requestScopedPermission(action.intentSender, action.forType)
      }

  private fun requestScopedPermission(intentSender: IntentSender, requestType: ModificationType) {
    val requestCode = when (requestType) {
      ModificationType.UPDATE -> REQUEST_PERMISSION_UPDATE
      ModificationType.DELETE -> REQUEST_PERMISSION_DELETE
    }

    startIntentSenderForResult(intentSender, requestCode, null, 0, 0,
        0, null)
  }

  private fun imageChanged(toastMsg: String) {
    Snackbar.make(binding.root, toastMsg, Snackbar.LENGTH_LONG).show()
    navigateToMain(requireView())
  }

  private fun setupToolbar() {
    val appCompatActivity = activity as AppCompatActivity
    appCompatActivity.setSupportActionBar(binding.tbDetails)
    appCompatActivity.supportActionBar?.title = ""
    appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
    appCompatActivity.supportActionBar?.setDisplayShowHomeEnabled(true)
  }

  private fun saveMemeAs() {
    val format = getImageFormat(
        requireActivity().contentResolver.getType(image.uri)!!)
    val extension = getImageExtension(format)
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
      addCategory(Intent.CATEGORY_OPENABLE)
      putExtra(Intent.EXTRA_TITLE, "${System.currentTimeMillis()}.$extension")
      type = "image/*"
    }

    startActivityForResult(intent, REQUEST_SAVE_AS)
  }

  private fun showHideMemeBuilder() {
    if (binding.etTitle.visibility == View.VISIBLE) {
      hideMemeBuilder()
    } else {
      hideInfoImage()
      showMemeBuilder()
    }
  }

  private fun showMemeBuilder() {
    binding.etTitle.visibility = View.VISIBLE
    binding.etSubtitle.visibility = View.VISIBLE

    binding.etTitle.requestFocus()
    showKeyboard()
  }

  private fun hideMemeBuilder() {
    binding.etTitle.visibility = View.INVISIBLE
    binding.etSubtitle.visibility = View.INVISIBLE
  }

  private fun updateMeme() {
    hideKeyboard {
      hideMemeBuilder()
      viewModel.updateImage(image, createBitmap())
    }
  }

  private fun saveMeme(uri: Uri?) {
    hideKeyboard {
      hideMemeBuilder()
      viewModel.saveImage(image, uri, createBitmap())
    }
  }

  private fun createBitmap(): Bitmap {
    val bitmap: Bitmap = getBitmapFromView(binding.ivImage)
    addTextToBitmap(bitmap, binding.etTitle.text.toString(), binding.etSubtitle.text.toString())
    return bitmap
  }

  private fun getBitmapFromView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    view.draw(Canvas(bitmap))
    return bitmap
  }

  private fun addTextToBitmap(viewBitmap: Bitmap, topText: String, bottomText: String) {
    val bitmapWidth = viewBitmap.width
    val pictureCanvas = Canvas(viewBitmap)

    val textPaint = binding.etTitle.paint
    textPaint.color = Color.WHITE
    textPaint.textAlign = Paint.Align.CENTER

    val textPaintOutline = Paint()
    textPaintOutline.isAntiAlias = true
    textPaintOutline.textSize = binding.etTitle.textSize
    textPaintOutline.color = Color.BLACK
    textPaintOutline.typeface = binding.etTitle.typeface
    textPaintOutline.style = Paint.Style.STROKE
    textPaintOutline.textAlign = Paint.Align.CENTER
    textPaintOutline.strokeWidth = 10f

    val xPos = (bitmapWidth / 2).toFloat()
    var yPos = binding.etTitle.pivotY + binding.etTitle.height

    pictureCanvas.drawText(topText, xPos, yPos, textPaintOutline)
    pictureCanvas.drawText(topText, xPos, yPos, textPaint)

    yPos = binding.ivImage.height.toFloat() - binding.etSubtitle.height

    pictureCanvas.drawText(bottomText, xPos, yPos, textPaintOutline)
    pictureCanvas.drawText(bottomText, xPos, yPos, textPaint)
  }

  @SuppressLint("SetTextI18n")
  private fun showHideInfoImage() {
    if (binding.llInfo.visibility == View.VISIBLE) {
      hideInfoImage()
    } else {
      hideMemeBuilder()

      if (hasMediaLocationPermission(requireContext())) {
        setImageLocation()
      } else {
        requestMediaLocationPermission(
            requireActivity(),
            REQUEST_PERMISSION_MEDIA_ACCESS)
        return
      }

      showInfoImage()

      binding.tvDate.text = getFormattedDateFromMillis(image.date.toLong())
      binding.tvPath.text = image.path
      binding.tvSize.text = getFormattedKbFromBytes(requireContext(), image.size.toLong())

      if (image.width == null || image.height == null) {
        binding.tvDimensions.visibility = View.GONE

      } else {
        binding.tvDimensions.visibility = View.VISIBLE
        binding.tvDimensions.text = getString(
            R.string.image_dimensions, image.width, image.height)
      }
    }
  }

  private fun showInfoImage() {
    binding.llInfo.visibility = View.VISIBLE
  }

  private fun hideInfoImage() {
    binding.llInfo.visibility = View.INVISIBLE
  }

  private fun setImageLocation() {
    if (!hasSdkHigherThan(Build.VERSION_CODES.P)) {
      binding.tvLocation.visibility = View.GONE
      return
    }
  }

  private fun showKeyboard() {
    val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(binding.etTitle, InputMethodManager.SHOW_FORCED)
  }

  private fun hideKeyboard(action: (() -> Unit)? = null) {
    if (view == null) {
      action?.invoke()

    } else {
      val imm = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(requireView().windowToken, 0)

      // We need to wait a couple of milliseconds for the keyboard to hide otherwise we won't
      // have the image filling the entire screen.
      if (action != null) {
        requireView().postDelayed({ action() },
            KEYBOARD_HIDDEN_DELAY)
      }
    }
  }
}