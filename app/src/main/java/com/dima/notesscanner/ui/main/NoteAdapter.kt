package com.dima.notesscanner.ui.main

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dima.notesscanner.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val notes: List<MainFragment.NoteItem>,
    private val onItemClick: (MainFragment.NoteItem) -> Unit,
    private val onSelectionChanged: () -> Unit,
    private val onCloudClick: (MainFragment.NoteItem) -> Unit,
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rootLayout: LinearLayout = itemView.findViewById(R.id.rootLayout)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvSize: TextView = itemView.findViewById(R.id.tvSize)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val ivPreview: ImageView = itemView.findViewById(R.id.ivPreview)
        val btnCloud: ImageButton = itemView.findViewById(R.id.btnCloud)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        // Загружаем превью, если есть
        if (note.previewPath != null) {
            Glide.with(holder.itemView.context)
                .load(note.previewPath)
                .centerCrop()
                .placeholder(R.drawable.ic_image)
                .into(holder.ivPreview)
        } else {
            holder.ivPreview.setImageResource(R.drawable.ic_image)
        }

        holder.tvTitle.text = note.name.removeSuffix(".pdf").removeSuffix(".PDF")
        holder.tvDate.text = formatDate(note.lastModified)
        holder.tvSize.text = String.format("%.2f MB", note.sizeMB)

        // Чередование цветов фона
        val backgroundColor = if (position % 2 == 0) {
            Color.parseColor("#2644F7")  // чётные элементы
        } else {
            Color.parseColor("#536BFF")  // нечётные элементы
        }
        holder.rootLayout.setBackgroundColor(backgroundColor)

        // Обработка клика по элементу
        holder.itemView.setOnClickListener {
            onItemClick(note)
        }

        // Обработка чекбокса (отключаем старый слушатель, чтобы избежать конфликтов)
        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = note.isSelected
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            note.isSelected = isChecked
            onSelectionChanged()
        }

        if(note.isUploaded == true){
            holder.btnCloud.setImageResource(R.drawable.ic_cloud_done)
        }
        holder.btnCloud.setOnClickListener {
            onCloudClick(note)
        }
    }

    override fun getItemCount() = notes.size

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
}