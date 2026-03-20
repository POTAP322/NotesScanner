package com.dima.notesscanner.ui.main

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dima.notesscanner.R
import java.io.File

class PhotosAdapter(
    private var photos: MutableList<File>,
    private val onItemClick: (File) -> Unit,
    private val onItemLongClick: (File) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photos[position]

        // Загружаем фото
        Glide.with(holder.itemView.context)
            .load(photoFile)
            .centerCrop()
            .placeholder(R.drawable.ic_broken_image)
            .into(holder.ivPhoto)

        // Обработка нажатий
        holder.itemView.setOnClickListener {
            onItemClick(photoFile)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(photoFile)
            true
        }
    }

    override fun getItemCount() = photos.size
}