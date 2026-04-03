package com.dima.notesscanner.utils

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileNameDialog(private val context: Context) {

    private fun getDefaultName(): String {
        val format = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        return "notes_${format.format(Date())}"
    }

    fun show(onResult: (String) -> Unit) {
        val editText = EditText(context).apply {
            setText(getDefaultName())
            selectAll()
            hint = "Введите название файла"
        }

        AlertDialog.Builder(context)
            .setTitle("Сохранить PDF")
            .setMessage("Введите имя файла")
            .setView(editText)
            .setPositiveButton("Сохранить") { _, _ ->
                val fileName = editText.text.toString()
                    .trim()
                    .ifEmpty { getDefaultName() }
                    .replace(" ", "_") // заменяем пробелы на _

                // Добавляем .pdf если забыли
                val finalName = if (fileName.endsWith(".pdf")) fileName else "$fileName.pdf"
                onResult(finalName)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}