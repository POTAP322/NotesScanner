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
import android.widget.SeekBar
import android.widget.TextView
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        setupSeekBars(view)
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

    private fun applyBrightnessAndContrast(brightnessValue: Int, contrastValue: Int) {
        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage("Применение эффектов...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Загружаем Bitmap через Glide
                val originalBitmap = Glide.with(this@ImageEditingFragment)
                    .asBitmap()
                    .load(originalPhotoFile ?: photoFile)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit()
                    .get()

                if (originalBitmap != null) {
                    val resultBitmap = applyColorMatrix(originalBitmap, brightnessValue, contrastValue)

                    val outputStream = FileOutputStream(photoFile)
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    // Очищаем память
                    originalBitmap.recycle()
                    resultBitmap.recycle()

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()

                        val ivPhoto: ImageView = requireView().findViewById(R.id.mainImage)
                        Glide.with(this@ImageEditingFragment)
                            .load(photoFile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivPhoto)

                        sharedGalleryViewModel.notifyPhotoChanged(photoFile)
                        Toast.makeText(requireContext(), "Эффекты применены", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyColorMatrix(bitmap: Bitmap, brightnessValue: Int, contrastValue: Int): Bitmap {
        // Преобразуем значения пользователя (-100..100) в коэффициенты
        val brightness = 1.0f + (brightnessValue / 100.0f)  // 0.0..2.0, 1.0 = норма
        val contrast = 1.0f + (contrastValue / 100.0f)      // 0.0..2.0, 1.0 = норма

        // Создаём матрицу яркости
        val brightnessMatrix = ColorMatrix().apply {
            setScale(brightness, brightness, brightness, 1.0f)
        }

        // Создаём матрицу контраста
        val contrastMatrix = ColorMatrix().apply {
            setScale(contrast, contrast, contrast, 1.0f)
        }

        // Объединяем матрицы
        val colorMatrix = ColorMatrix()
        colorMatrix.setConcat(contrastMatrix, brightnessMatrix)

        // Создаём фильтр
        val filter = ColorMatrixColorFilter(colorMatrix)

        // Создаём пустой Bitmap для результата
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(resultBitmap)
        val paint = android.graphics.Paint().apply {
            colorFilter = filter
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return resultBitmap
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
    private fun setupSeekBars(view: View) {
        // Ползунок яркости
        val sbBrightness = view.findViewById<SeekBar>(R.id.sbBrightness)
        val tvBrightnessValue = view.findViewById<TextView>(R.id.tvBrightnessValue)

        sbBrightness.max = 200
        sbBrightness.progress = 100  // 100 = 0 = норма
        tvBrightnessValue.text = "0"

        // Ползунок контраста
        val sbContrast = view.findViewById<SeekBar>(R.id.sbContrast)
        val tvContrastValue = view.findViewById<TextView>(R.id.tvContrastValue)

        sbContrast.max = 200
        sbContrast.progress = 100  // 0 = норма
        tvContrastValue.text = "0"

        sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 100
                tvBrightnessValue.text = value.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val brightnessValue = sbBrightness.progress - 100
                val contrastValue = sbContrast.progress - 100
                applyBrightnessAndContrast(brightnessValue, contrastValue)
            }
        })

        sbContrast.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = progress - 100
                tvContrastValue.text = value.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val brightnessValue = sbBrightness.progress - 100
                val contrastValue = sbContrast.progress - 100
                applyBrightnessAndContrast(brightnessValue, contrastValue)
            }
        })
    }

}