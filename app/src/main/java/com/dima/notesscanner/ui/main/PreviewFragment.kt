package com.dima.notesscanner.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.dima.notesscanner.R
import com.dima.notesscanner.utils.FileNameDialog
import com.dima.notesscanner.utils.PdfGenerator
import com.dima.notesscanner.viewmodel.SharedGalleryViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.File

class PreviewFragment : Fragment() {

    private lateinit var recyclerview: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var pageCount: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button
    private lateinit var btnFinish: Button
    private lateinit var photosAdapter: PreviewPhotosAdapter
    private val photosList = mutableListOf<File>()

    private val sharedGalleryViewModel: SharedGalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupToolbar(view)
        setupButtons()
        setupPhotosList()
    }

    private fun initViews(view: View) {
        recyclerview = view.findViewById(R.id.recyclerView)
        tabLayout = view.findViewById(R.id.tabLayout)
        pageCount = view.findViewById(R.id.pageCount)
        btnSave = view.findViewById(R.id.btnSave)
        btnShare = view.findViewById(R.id.btnShare)
        btnFinish = view.findViewById(R.id.btnFinish)
    }

    private fun setupToolbar(view: View) {
        view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener {
                findNavController().navigateUp()
            }
    }

    private fun setupButtons() {
        btnSave.setOnClickListener {
            val photos = sharedGalleryViewModel.allPhotos.value

            if (photos.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Нет фото для сохранения", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            FileNameDialog(requireContext()).show { fileName ->
                lifecycleScope.launch {
                    val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                        setMessage("Создание PDF...")
                        setCancelable(false)
                        show()
                    }

                    val generator = PdfGenerator(requireContext())
                    val result = generator.savePdf(photos, fileName)

                    progressDialog.dismiss()

                    result.onSuccess { uri ->
                        Toast.makeText(requireContext(), "PDF сохранен: $fileName", Toast.LENGTH_LONG).show()
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnShare.setOnClickListener {
            val photos = sharedGalleryViewModel.allPhotos.value

            if (photos.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Нет фото для отправки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            FileNameDialog(requireContext()).show { fileName ->
                lifecycleScope.launch {
                    val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                        setMessage("Подготовка к отправке...")
                        setCancelable(false)
                        show()
                    }

                    val generator = PdfGenerator(requireContext())
                    val result = generator.createShareablePdf(photos, fileName)

                    progressDialog.dismiss()

                    result.onSuccess { uri ->
                        // Создаем Intent для отправки
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Отправить PDF"))
                    }.onFailure { error ->
                        Toast.makeText(
                            requireContext(),
                            "Ошибка: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

        }
        btnFinish.setOnClickListener {
            finishAndClean()
        }


    }

    private fun setupPhotosList() {
        val photos = sharedGalleryViewModel.allPhotos.value ?: emptyList()
        photosList.clear()
        photosList.addAll(photos)

        pageCount.text = "Страниц: ${photosList.size}"

        // Используем RecyclerView вместо ViewPager для списка с кнопками
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        photosAdapter = PreviewPhotosAdapter(
            photos = photosList,
            onMoveUp = { position ->
                photosAdapter.moveItemUp(position)
                updatePageNumbers()
            },
            onMoveDown = { position ->
                photosAdapter.moveItemDown(position)
                updatePageNumbers()
            }
        )

        recyclerView?.adapter = photosAdapter
    }

    private fun updatePageNumbers() {
        pageCount.text = "Страниц: ${photosList.size}"
        // Сохраняем новый порядок в ViewModel
        sharedGalleryViewModel.updatePhotosOrder(photosList)
    }

    private fun finishAndClean() {
        // Диалог подтверждения
        AlertDialog.Builder(requireContext())
            .setTitle("Завершить сессию")
            .setMessage("Все несохранённые фото будут удалены.")
            .setPositiveButton("Завершить") { _, _ ->
                performCleanup()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performCleanup() {
        // Получаем все фото
        val photos = sharedGalleryViewModel.allPhotos.value ?: emptyList()

        // Удаляем файлы с диска
        var deletedCount = 0
        photos.forEach { photoFile ->
            try {
                if (photoFile.exists()) {
                    photoFile.delete()
                    deletedCount++
                }
            } catch (e: Exception) {
                // Игнорируем ошибки удаления
            }
        }

        // Очищаем ViewModel
        sharedGalleryViewModel.clearAllPhotos()

        // Показываем сообщение
        Toast.makeText(requireContext(), "Удалено $deletedCount фото", Toast.LENGTH_SHORT).show()

        // Переходим на главный экран
        findNavController().navigate(R.id.action_preview_to_main)
    }
}