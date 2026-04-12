package com.dima.notesscanner.ui.main

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dima.notesscanner.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteAdapter(
    private val notes: List<MainFragment.NoteItem>,
    private val onItemClick: (MainFragment.NoteItem) -> Unit
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvSize: TextView = itemView.findViewById(R.id.tvSize)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]

        holder.tvTitle.text = note.name.removeSuffix(".pdf").removeSuffix(".PDF")
        holder.tvDate.text = formatDate(note.lastModified)
        holder.tvSize.text = String.format("%.2f MB", note.sizeMB)

        holder.itemView.setOnClickListener {
            onItemClick(note)
        }
    }

    override fun getItemCount() = notes.size

    private fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }
}