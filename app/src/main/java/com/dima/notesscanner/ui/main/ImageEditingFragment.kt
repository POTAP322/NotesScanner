package com.dima.notesscanner.ui.main

import android.graphics.Bitmap
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Mat
import androidx.core.graphics.createBitmap

class ImageEditingFragment : Fragment() {

    private lateinit var photoFile: File
    private var originalPhotoFile: File? = null   // Для кнопки "Назад"
    private var basePhotoFile: File? = null       // Для ползунков (обновляется после обрезки)

    private val sharedGalleryViewModel: SharedGalleryViewModel by activityViewModels()
    private var isApplying = false

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
            // Оригинал (не меняется)
            originalPhotoFile = File(requireContext().cacheDir, "backup_original_${System.currentTimeMillis()}.jpg")
            photoFile.copyTo(originalPhotoFile!!, overwrite = true)
            // База для ползунков (меняется после обрезки)
            basePhotoFile = File(requireContext().cacheDir, "backup_base_${System.currentTimeMillis()}.jpg")
            photoFile.copyTo(basePhotoFile!!, overwrite = true)
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

                    // Обновляем только basePhotoFile
                    basePhotoFile?.delete()
                    basePhotoFile = File(requireContext().cacheDir, "backup_base_${System.currentTimeMillis()}.jpg")
                    photoFile.copyTo(basePhotoFile!!, overwrite = true)

                    sharedGalleryViewModel.notifyPhotoChanged(photoFile)

                    val ivPhoto: ImageView = requireView().findViewById(R.id.mainImage)
                    Glide.with(this)
                        .load(photoFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(ivPhoto)

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
                    outputCompressQuality = 90,
                    toolbarColor = ContextCompat.getColor(requireContext(), R.color.item_color_2),
                    toolbarTitleColor = ContextCompat.getColor(requireContext(), R.color.white),
                    toolbarBackButtonColor = ContextCompat.getColor(requireContext(), R.color.white),
                    activityBackgroundColor = ContextCompat.getColor(requireContext(), R.color.white)

                )
            )
        )
    }

    private fun applyBrightnessAndContrast(brightnessValue: Int, contrastValue: Int) {
        // Защита от повторного вызова
        if (isApplying) {
            Toast.makeText(requireContext(), "Подождите, предыдущая операция ещё не завершена", Toast.LENGTH_SHORT).show()
            return
        }

        isApplying = true

        // Показываем прогресс
        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage("Применение эффектов...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Загружаем Bitmap с уменьшенным разрешением для обработки
                val originalBitmap = Glide.with(this@ImageEditingFragment)
                    .asBitmap()
                    .load(basePhotoFile ?: photoFile)  // ← загружаем basePhotoFile
                    .override(1024, 1024)  // Уменьшаем размер для обработки (быстрее)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit()
                    .get()

                if (originalBitmap != null) {
                    val resultBitmap = applyBrightnessAndContrast(originalBitmap, brightnessValue, contrastValue)

                    // Сохраняем в файл
                    val outputStream = FileOutputStream(photoFile)
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    // Очищаем память
                    originalBitmap.recycle()
                    resultBitmap.recycle()

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        isApplying = false

                        // Обновляем ImageView
                        val ivPhoto: ImageView = requireView().findViewById(R.id.mainImage)
                        Glide.with(this@ImageEditingFragment)
                            .load(photoFile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivPhoto)

                        sharedGalleryViewModel.notifyPhotoChanged(photoFile)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        isApplying = false
                        Toast.makeText(requireContext(), "Ошибка загрузки фото", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    isApplying = false
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyBrightnessAndContrast(originalBitmap: Bitmap, brightnessValue: Int, contrastValue: Int): Bitmap {

        val contrast = 1.0 + (contrastValue / 100.0)
        val brightness = brightnessValue.toDouble()

        val sourceMat = Mat()
        Utils.bitmapToMat(originalBitmap,sourceMat)

        val destinationMat = Mat()

        //формула g(x) = alpha * f(x) + beta
        sourceMat.convertTo(destinationMat,-1,contrast,brightness)

        val resultBitmap = createBitmap(destinationMat.cols(), destinationMat.rows(), Bitmap.Config.ARGB_8888)

        Utils.matToBitmap(destinationMat,resultBitmap)

        sourceMat.release()
        destinationMat.release()


        return resultBitmap

    }

    private fun setupButtons(view: View) {
        // Кнопка "Назад" отменяем изменения
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            // Восстанавливаем оригинальный файл
            originalPhotoFile?.let { backup ->
                if (backup.exists()) {
                    backup.copyTo(photoFile, overwrite = true)
                    // Также обновляем basePhotoFile
                    backup.copyTo(basePhotoFile!!, overwrite = true)
                    sharedGalleryViewModel.notifyPhotoChanged(photoFile)
                }
            }
            findNavController().navigate(R.id.action_image_editing_to_image_processing)
        }

        // Кнопка "Готово" — сохраняем изменения и удаляем бэкап
        view.findViewById<ImageButton>(R.id.btnDone).setOnClickListener {
            originalPhotoFile?.delete()
            basePhotoFile?.delete()
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