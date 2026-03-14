package com.dima.notesscanner.ui.main

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dima.notesscanner.R
import java.io.File

class PreviewPhotosAdapter(
    private var photos: MutableList<File>,
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit
) : RecyclerView.Adapter<PreviewPhotosAdapter.PreviewViewHolder>() {

    class PreviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

        // Загружаем фото
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        holder.ivPhoto.setImageBitmap(bitmap)


        // ВАЖНО: Убираем старые слушатели перед установкой новых
        holder.btnMoveUp.setOnClickListener(null)
        holder.btnMoveDown.setOnClickListener(null)

        // Кнопка вверх - активна только если не первый элемент
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

        // Кнопка вниз - активна только если не последний элемент
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
            // Обновляем соседние элементы, чтобы кнопки перерисовались
            notifyItemChanged(position - 1)
            notifyItemChanged(position)
        }
    }

    fun moveItemDown(position: Int) {
        if (position < photos.size - 1) {
            val item = photos.removeAt(position)
            photos.add(position + 1, item)
            notifyItemMoved(position, position + 1)
            // Обновляем соседние элементы
            notifyItemChanged(position)
            notifyItemChanged(position + 1)
        }
    }

    fun getCurrentList(): List<File> = photos
}