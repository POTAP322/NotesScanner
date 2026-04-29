package com.dima.notesscanner.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dima.notesscanner.R

class MainFragment : Fragment() {

    private lateinit var rvNotes: RecyclerView
    private lateinit var btnToProcessing: Button
    private lateinit var adapter: NoteAdapter
    private val notesList = mutableListOf<NoteItem>()

    private lateinit var normalPanel: ConstraintLayout
    private lateinit var selectionPanel: ConstraintLayout

    // Класс для хранения информации о PDF
    data class NoteItem(
        val name: String,
        val uri: Uri,
        val lastModified: Long,
        val sizeMB: Double,
        var isSelected: Boolean = false
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)
        setupItems(view)
        loadNotes()
    }

    private fun setupRecyclerView(view: View) {
        rvNotes = view.findViewById(R.id.rvNotes)
        rvNotes.layoutManager = LinearLayoutManager(requireContext())

        adapter = NoteAdapter(notesList,
            onItemClick = { note -> openPdf(note.uri) },
            onSelectionChanged = { updateSelectionUI() }
        )
        rvNotes.adapter = adapter
    }

    private fun setupItems(view: View) {
        normalPanel = view.findViewById(R.id.normalPanel)
        selectionPanel = view.findViewById(R.id.selectionPanel)

        btnToProcessing = view.findViewById(R.id.btnToProcessing)
        btnToProcessing.setOnClickListener {
            findNavController().navigate(R.id.action_main_to_image_processing)
        }


    }

    private fun loadNotes() {
        notesList.clear()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            loadNotesWithMediaStore()
        } else {
            loadNotesWithFileSystem()
        }
    }

    private fun loadNotesWithMediaStore() {
        val resolver = requireContext().contentResolver
        val uri = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE          // ← добавили размер
        )

        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? AND " +
                "(${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR " +
                "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?)"
        val selectionArgs = arrayOf("%.pdf", "application/pdf", "%.PDF")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        val cursor = resolver.query(uri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val idColumn = it.getColumnIndex(MediaStore.Files.FileColumns._ID)
            val nameColumn = it.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateColumn = it.getColumnIndex(MediaStore.Files.FileColumns.DATE_MODIFIED)
            val sizeColumn = it.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val lastModified = it.getLong(dateColumn) * 1000
                val sizeBytes = it.getLong(sizeColumn)
                val sizeMB = sizeBytes / (1024.0 * 1024.0)  // байты → МБ

                val fileUri = Uri.withAppendedPath(uri, id.toString())

                notesList.add(NoteItem(name, fileUri, lastModified, sizeMB))
            }
        }

        adapter.notifyDataSetChanged()
    }

    @Suppress("DEPRECATION")
    private fun loadNotesWithFileSystem() {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val pdfFiles = downloadsDir.listFiles { file ->
            file.extension.equals("pdf", ignoreCase = true)
        }

        pdfFiles?.sortedByDescending { it.lastModified() }?.forEach { file ->
            val uri = Uri.fromFile(file)
            val sizeMB = file.length() / (1024.0 * 1024.0)
            notesList.add(NoteItem(file.name, uri, file.lastModified(), sizeMB))
        }

        adapter.notifyDataSetChanged()
    }

    private fun openPdf(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            }

            startActivity(Intent.createChooser(intent, "Открыть PDF"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Не удалось открыть PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    private fun updateSelectionUI() {
        val selectedCount = notesList.count { it.isSelected }
        if (selectedCount > 0) {
            normalPanel.visibility = View.GONE
            selectionPanel.visibility = View.VISIBLE
        } else {
            normalPanel.visibility = View.VISIBLE
            selectionPanel.visibility = View.GONE
        }
    }

}