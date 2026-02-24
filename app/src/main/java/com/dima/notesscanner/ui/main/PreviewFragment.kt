package com.dima.notesscanner.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.dima.notesscanner.R
import com.dima.notesscanner.utils.PdfGenerator
import com.dima.notesscanner.viewmodel.SharedGalleryViewModel
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch
import java.io.File

class PreviewFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var pageCount: TextView
    private lateinit var btnSave: Button
    private lateinit var btnShare: Button

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
        setupViewPager()
    }

    private fun initViews(view: View) {
        viewPager = view.findViewById(R.id.viewPager2)
        tabLayout = view.findViewById(R.id.tabLayout)
        pageCount = view.findViewById(R.id.pageCount)
        btnSave = view.findViewById(R.id.btnSave)
        btnShare = view.findViewById(R.id.btnShare)
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
                Toast.makeText(requireContext(), "Нет фото для сохранения", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                    setMessage("Создание PDF...")
                    setCancelable(false)
                    show()
                }

                val generator = PdfGenerator(requireContext())
                val result = generator.savePdf(photos)

                progressDialog.dismiss()

                result.onSuccess { uri ->
                    // Извлекаем имя файла из Uri
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "PDF"
                    Toast.makeText(requireContext(), "PDF сохранен: $fileName", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "Ошибка сохранения: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnShare.setOnClickListener {
            val photos = sharedGalleryViewModel.allPhotos.value

            if (photos.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "Нет фото для отправки", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val progressDialog = android.app.ProgressDialog(requireContext()).apply {
                    setMessage("Подготовка к отправке...")
                    setCancelable(false)
                    show()
                }

                val generator = PdfGenerator(requireContext())
                val result = generator.createShareablePdf(photos)

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
                    Toast.makeText(requireContext(), "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupViewPager() {
        val photos = sharedGalleryViewModel.allPhotos.value ?: emptyList()
        pageCount.text = "Страниц: ${photos.size}"

        if (photos.isNotEmpty()) {
            // Позже добавишь адаптер
            // viewPager.adapter = PhotoPagerAdapter(photos)
        }
    }
}