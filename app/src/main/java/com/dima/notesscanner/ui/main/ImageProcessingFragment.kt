package com.dima.notesscanner.ui.main

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dima.notesscanner.R
import com.dima.notesscanner.utils.FileNameDialog
import com.dima.notesscanner.utils.PdfGenerator
import com.dima.notesscanner.viewmodel.SharedGalleryViewModel
import kotlinx.coroutines.launch
import java.io.File

class ImageProcessingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotosAdapter
    private val photosList = mutableListOf<File>()
    private val sharedGalleryViewModel: SharedGalleryViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_image_processing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)

        sharedGalleryViewModel.allPhotos.observe(viewLifecycleOwner) { allPhotos ->
            photosList.clear()
            photosList.addAll(allPhotos)
            adapter?.notifyDataSetChanged()
        }

        sharedGalleryViewModel.capturedPhotos.observe(viewLifecycleOwner) { newPhotos ->
            if (newPhotos.isNotEmpty()) sharedGalleryViewModel.clearPhotos()
        }

        setupButtons(view)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PhotosAdapter(
            photos = photosList,
            onItemClick = { photoFile ->
                val bundle = Bundle().apply { putString("photoPath", photoFile.absolutePath) }
                findNavController().navigate(R.id.action_image_processing_to_image_editing, bundle)
            },
            onItemLongClick = { photoFile -> showDeleteDialog(photoFile) },
            onMoveUp = { position -> adapter.moveItemUp(position) },
            onMoveDown = { position -> adapter.moveItemDown(position) }
        )
        recyclerView.adapter = adapter
    }

    private fun setupButtons(view: View) {
        view.findViewById<ImageButton>(R.id.btnToCamera).setOnClickListener {
            findNavController().navigate(R.id.action_image_processing_to_camera)
        }

        val btnMenu = view.findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener { v ->
            showPopupMenu(v)
        }
    }

    private fun showPopupMenu(anchor: View) {
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.menu_popup, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popupWindow.elevation = 10f

        // Измеряем размеры окна
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val x = location[0] + anchor.width / 2 - popupWidth / 2
        val y = location[1] - popupHeight - 40

        popupWindow.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)

        // Получаем кнопки внутри меню
        val btnSave = popupView.findViewById<ImageButton>(R.id.btnSave)
        val btnShare = popupView.findViewById<ImageButton>(R.id.btnShare)
        val btnFinish = popupView.findViewById<ImageButton>(R.id.btnFinish)

        // Скрываем их изначально
        btnSave.visibility = View.INVISIBLE
        btnShare.visibility = View.INVISIBLE
        btnFinish.visibility = View.INVISIBLE

        // Анимируем появление с задержкой
        val startDelay = 50L
        val delayBetween = 80L

        animateButton(btnFinish, startDelay)
        animateButton(btnShare, startDelay + delayBetween)
        animateButton(btnSave, startDelay + delayBetween * 2)

        btnSave.setOnClickListener {
            savePdf()
            popupWindow.dismiss()
        }
        btnShare.setOnClickListener {
            sharePdf()
            popupWindow.dismiss()
        }
        btnFinish.setOnClickListener {
            finishAndClean()
            popupWindow.dismiss()
        }
    }

    private fun animateButton(button: View, delay: Long) {
        button.visibility = View.VISIBLE
        button.alpha = 0f
        button.scaleX = 0.5f
        button.scaleY = 0.5f

        button.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setStartDelay(delay)
            .start()
    }

    // --- функции из PreviewFragment ---
    private fun savePdf() {
        val photos = sharedGalleryViewModel.allPhotos.value
        if (photos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Нет фото для сохранения", Toast.LENGTH_SHORT).show()
            return
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
                result.onSuccess { Toast.makeText(requireContext(), "PDF сохранен: $fileName", Toast.LENGTH_LONG).show() }
                    .onFailure { Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun sharePdf() {
        val photos = sharedGalleryViewModel.allPhotos.value
        if (photos.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Нет фото для отправки", Toast.LENGTH_SHORT).show()
            return
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
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Отправить PDF"))
                }.onFailure { Toast.makeText(requireContext(), "Ошибка: ${it.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun finishAndClean() {
        AlertDialog.Builder(requireContext())
            .setTitle("Завершить сессию")
            .setMessage("Все несохранённые фото будут удалены.")
            .setPositiveButton("Завершить") { _, _ -> performCleanup() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performCleanup() {
        val photos = sharedGalleryViewModel.allPhotos.value ?: emptyList()
        var deletedCount = 0
        photos.forEach { photoFile ->
            try { if (photoFile.exists()) { photoFile.delete(); deletedCount++ } } catch (e: Exception) { }
        }
        sharedGalleryViewModel.clearAllPhotos()
        Toast.makeText(requireContext(), "Удалено $deletedCount фото", Toast.LENGTH_SHORT).show()
        findNavController().navigate(R.id.action_image_processing_to_main) // надо создать действие
    }

    private fun showDeleteDialog(photoFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить фото")
            .setMessage("Вы уверены?")
            .setPositiveButton("Удалить") { _, _ ->
                sharedGalleryViewModel.removePhoto(photoFile)
                photoFile.delete()
                Toast.makeText(requireContext(), "Фото удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}