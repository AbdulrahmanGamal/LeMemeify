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

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.raywenderlich.android.lememeify.R
import com.raywenderlich.android.lememeify.model.Media
import kotlinx.android.synthetic.main.item_image.view.*

class MediaAdapter(val clickAction: (Media) -> Unit) :
    ListAdapter<Media, MediaAdapter.MainViewHolder>(DiffCallback()) {

  var tracker: SelectionTracker<String>? = null

  init {
    setHasStableIds(true)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return MainViewHolder(inflater.inflate(R.layout.item_image, parent, false))
  }

  override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
    val imageToBind = getItem(position)

    tracker?.let {
      Glide.with(holder.image.context)
          .load(imageToBind.uri)
          .signature(ObjectKey(imageToBind.date))
          .into(holder.image)

      if(it.isSelected("${imageToBind.id}")) {
        holder.image.setColorFilter(
            ContextCompat.getColor(holder.image.context, R.color.color65TransparentPrimary),
            PorterDuff.Mode.SRC_OVER)
      } else {
        holder.image.clearColorFilter()
      }

      holder.image.setOnClickListener {
        clickAction(imageToBind)
      }
    }
  }

  override fun getItemId(position: Int): Long {
    return currentList[position].id
  }

  private class DiffCallback : DiffUtil.ItemCallback<Media>() {
    override fun areItemsTheSame(oldItem: Media, newItem: Media): Boolean {
      return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Media, newItem: Media): Boolean {
      return oldItem == newItem
    }
  }

  inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val image = itemView.iv_image!!

    fun getImageDetails(): ItemDetailsLookup.ItemDetails<String> =

        object : ItemDetailsLookup.ItemDetails<String>() {

          override fun getPosition(): Int = adapterPosition

          override fun getSelectionKey(): String? = "${getItem(adapterPosition).id}"
        }
  }
}