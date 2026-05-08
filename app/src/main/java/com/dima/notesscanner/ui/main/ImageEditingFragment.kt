package com.dima.notesscanner.ui.main

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import com.github.chrisbanes.photoview.PhotoView

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

        val photoView = view.findViewById<PhotoView>(R.id.mainImage)
        Glide.with(photoView.context)
            .load(photoPath)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .placeholder(R.drawable.ic_broken_image)
            .into(photoView)

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

                    val photoView = requireView().findViewById<PhotoView>(R.id.mainImage)
                    Glide.with(this)
                        .load(photoFile)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(photoView)

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
        if (isApplying) {
            Toast.makeText(requireContext(), "Подождите, предыдущая операция ещё не завершена", Toast.LENGTH_SHORT).show()
            return
        }

        isApplying = true

        val progressDialog = android.app.ProgressDialog(requireContext()).apply {
            setMessage("Применение эффектов...")
            setCancelable(false)
            show()
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val originalBitmap = Glide.with(this@ImageEditingFragment)
                    .asBitmap()
                    .load(basePhotoFile ?: photoFile)
                    .override(1024, 1024)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .submit()
                    .get()

                if (originalBitmap != null) {
                    val resultBitmap = applyBrightnessAndContrastOpenCV(originalBitmap, brightnessValue, contrastValue)

                    val outputStream = FileOutputStream(photoFile)
                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    originalBitmap.recycle()
                    resultBitmap.recycle()

                    withContext(Dispatchers.Main) {
                        progressDialog.dismiss()
                        isApplying = false

                        val photoView = requireView().findViewById<PhotoView>(R.id.mainImage)
                        Glide.with(this@ImageEditingFragment)
                            .load(photoFile)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(photoView)

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

    private fun applyBrightnessAndContrastOpenCV(originalBitmap: Bitmap, brightnessValue: Int, contrastValue: Int): Bitmap {
        val contrast = 1.0 + (contrastValue / 100.0)
        val brightness = brightnessValue.toDouble()

        val sourceMat = Mat()
        Utils.bitmapToMat(originalBitmap, sourceMat)

        val destinationMat = Mat()
        sourceMat.convertTo(destinationMat, -1, contrast, brightness)

        val resultBitmap = createBitmap(destinationMat.cols(), destinationMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(destinationMat, resultBitmap)

        sourceMat.release()
        destinationMat.release()

        return resultBitmap
    }

    private fun setupButtons(view: View) {
        view.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            originalPhotoFile?.let { backup ->
                if (backup.exists()) {
                    backup.copyTo(photoFile, overwrite = true)
                    backup.copyTo(basePhotoFile!!, overwrite = true)
                    sharedGalleryViewModel.notifyPhotoChanged(photoFile)
                }
            }
            findNavController().navigate(R.id.action_image_editing_to_image_processing)
        }

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
        val sbBrightness = view.findViewById<SeekBar>(R.id.sbBrightness)
        val tvBrightnessValue = view.findViewById<TextView>(R.id.tvBrightnessValue)

        sbBrightness.max = 200
        sbBrightness.progress = 100
        tvBrightnessValue.text = "0"

        val sbContrast = view.findViewById<SeekBar>(R.id.sbContrast)
        val tvContrastValue = view.findViewById<TextView>(R.id.tvContrastValue)

        sbContrast.max = 200
        sbContrast.progress = 100
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