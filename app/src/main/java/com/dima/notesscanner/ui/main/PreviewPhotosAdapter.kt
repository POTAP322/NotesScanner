package com.dima.notesscanner.ui.main

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dima.notesscanner.R
import java.io.File

class PreviewPhotosAdapter(
    private var photos: MutableList<File>,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit
) : RecyclerView.Adapter<PreviewPhotosAdapter.PreviewViewHolder>() {

    class PreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.rootLayout)
        val ivPhoto: ImageView = itemView.findViewById(R.id.ivPhoto)
        val btnMoveUp: ImageButton = itemView.findViewById(R.id.btnMoveUp)
        val btnMoveDown: ImageButton = itemView.findViewById(R.id.btnMoveDown)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_preview_photo, parent, false)
        return PreviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        val photoFile = photos[position]

        // Чередование цветов фона
        val backgroundColor = if (position % 2 == 0) {
            Color.parseColor("#2644F7")
        } else {
            Color.parseColor("#536BFF")
        }
        holder.rootLayout.setBackgroundColor(backgroundColor)

        // Загружаем фото
        Glide.with(holder.itemView.context)
            .load(photoFile)
            .centerCrop()
            .placeholder(R.drawable.ic_broken_image)
            .into(holder.ivPhoto)

        // Убираем старые слушатели
        holder.btnMoveUp.setOnClickListener(null)
        holder.btnMoveDown.setOnClickListener(null)

        // Кнопка вверх
        if (position > 0) {
            holder.btnMoveUp.isEnabled = true
            holder.btnMoveUp.alpha = 1.0f
            holder.btnMoveUp.setOnClickListener {
                onMoveUp(position)
            }
        } else {
            holder.btnMoveUp.isEnabled = false
            holder.btnMoveUp.alpha = 0.3f
        }

        // Кнопка вниз
        if (position < photos.size - 1) {
            holder.btnMoveDown.isEnabled = true
            holder.btnMoveDown.alpha = 1.0f
            holder.btnMoveDown.setOnClickListener {
                onMoveDown(position)
            }
        } else {
            holder.btnMoveDown.isEnabled = false
            holder.btnMoveDown.alpha = 0.3f
        }
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

    fun getCurrentList(): List<File> = photos
}