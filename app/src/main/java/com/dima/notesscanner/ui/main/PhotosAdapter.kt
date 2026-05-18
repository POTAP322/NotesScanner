package com.dima.notesscanner.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dima.notesscanner.R
import java.io.File

class PhotosAdapter(
    private var photos: MutableList<File>,
    private val onItemClick: (File) -> Unit,
    private val onItemLongClick: (File) -> Unit,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit
) : RecyclerView.Adapter<PhotosAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.rootLayout)
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val btnMoveUp: ImageButton = itemView.findViewById(R.id.btnMoveUp)
        val btnMoveDown: ImageButton = itemView.findViewById(R.id.btnMoveDown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoFile = photos[position]

        // Чередование цветов
        val backgroundColor = if (position % 2 == 0) Color.parseColor("#2644F7") else Color.parseColor("#536BFF")
        holder.rootLayout.setBackgroundColor(backgroundColor)

        Glide.with(holder.itemView.context)
            .load(photoFile)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .centerCrop()
            .placeholder(R.drawable.ic_broken_image)
            .into(holder.ivPhoto)

        // Клик по фото (открыть редактирование)
        holder.ivPhoto.setOnClickListener { onItemClick(photoFile) }
        // Длинный клик (удаление)
        holder.itemView.setOnLongClickListener { onItemLongClick(photoFile); true }

        // Стрелки
        holder.btnMoveUp.setOnClickListener { onMoveUp(position) }
        holder.btnMoveDown.setOnClickListener { onMoveDown(position) }
        holder.btnMoveUp.isEnabled = position > 0
        holder.btnMoveDown.isEnabled = position < photos.size - 1
        holder.btnMoveUp.alpha = if (position > 0) 1.0f else 0.3f
        holder.btnMoveDown.alpha = if (position < photos.size - 1) 1.0f else 0.3f
    }

    override fun getItemCount() = photos.size

    fun moveItemUp(position: Int) {
        if (position > 0) {
            val item = photos.removeAt(position)
            photos.add(position - 1, item)
            notifyItemMoved(position, position - 1)
            notifyItemChanged(position - 1)
            notifyItemChanged(position)
        }
    }

    fun moveItemDown(position: Int) {
        if (position < photos.size - 1) {
            val item = photos.removeAt(position)
            photos.add(position + 1, item)
            notifyItemMoved(position, position + 1)
            notifyItemChanged(position)
            notifyItemChanged(position + 1)
        }
    }
}