package com.dima.notesscanner.ui.main

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.dima.notesscanner.R
import java.io.File
import java.io.FileOutputStream
import com.dima.notesscanner.viewmodel.SharedGalleryViewModel

class ImageEditingFragment : Fragment() {

    private lateinit var photoFile: File
    private var originalPhotoFile: File? = null  // Сохраняем копию для отмены
    private val sharedGalleryViewModel: SharedGalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_editing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val photoPath = arguments?.getString("photoPath")
        if (photoPath != null) {
            photoFile = File(photoPath)
            // Создаём копию оригинального файла для отмены
            originalPhotoFile = File(requireContext().cacheDir, "backup_${System.currentTimeMillis()}.jpg")
            photoFile.copyTo(originalPhotoFile!!, overwrite = true)
        }

        val ivPhoto: ImageView = view.findViewById(R.id.mainImage)

        Glide.with(ivPhoto.context)
            .load(photoPath)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_broken_image)
            .into(ivPhoto)

        setupButtons(view)
    }

    @Suppress("DEPRECATION")
    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val croppedImageUri = result.uriContent
            if (croppedImageUri != null && ::photoFile.isInitialized) {
                try {
                    val inputStream = requireContext().contentResolver.openInputStream(croppedImageUri)
                    val outputStream = FileOutputStream(photoFile)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()

                    sharedGalleryViewModel.notifyPhotoChanged(photoFile)

                    val ivPhoto: ImageView = requireView().findViewById(R.id.mainImage)
                    Glide.with(this)
                        .load(photoFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(ivPhoto)

                    Toast.makeText(requireContext(), "Изображение обрезано", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            val exception = result.error
            Toast.makeText(requireContext(), "Ошибка при обрезке: ${exception?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtons(view: View) {
        // Кнопка "Назад" отменяем изменения
        view.findViewById<Button>(R.id.btnBack).setOnClickListener {
            // Восстанавливаем оригинальный файл
            originalPhotoFile?.let { backup ->
                if (backup.exists()) {
                    backup.copyTo(photoFile, overwrite = true)
                    backup.delete()
                    sharedGalleryViewModel.notifyPhotoChanged(photoFile)
                }
            }
            findNavController().navigate(R.id.action_image_editing_to_image_processing)
        }

        // Кнопка "Готово" — сохраняем изменения и удаляем бэкап
        view.findViewById<Button>(R.id.btnDone).setOnClickListener {
            originalPhotoFile?.delete()  // Удаляем бэкап
            findNavController().navigate(R.id.action_image_editing_to_image_processing)
        }

        val btnCrop = view.findViewById<ImageButton>(R.id.btnCrop)
        btnCrop.setOnClickListener {
            startCrop()
        }
    }

    private fun startCrop() {
        if (!::photoFile.isInitialized) {
            Toast.makeText(requireContext(), "Фото не загружено", Toast.LENGTH_SHORT).show()
            return
        }

        val imageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )

        cropImageLauncher.launch(
            CropImageContractOptions(
                uri = imageUri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    fixAspectRatio = false,
                    outputCompressFormat = Bitmap.CompressFormat.JPEG,
                    outputCompressQuality = 90
                )
            )
        )
    }
}