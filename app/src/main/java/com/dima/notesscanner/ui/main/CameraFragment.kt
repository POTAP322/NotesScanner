package com.dima.notesscanner.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dima.notesscanner.MainActivity
import com.dima.notesscanner.R
import java.io.File

class CameraFragment : Fragment() {

    private var imageCapture: ImageCapture? = null
    private lateinit var previewView: PreviewView
    private var camera: Camera? = null
    private var isFlashOn = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)

        if (!hasRequiredPermissions()) {
            requestPermissions(CAMERAX_PERMISSIONS, 0)
        } else {
            startCamera()

        }

        view.findViewById<Button>(R.id.btnBack2).setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_image_processing)
        }
        view.findViewById<ImageButton>(R.id.btnCapture).setOnClickListener {
            takePhoto()
        }
        view.findViewById<ImageButton>(R.id.btnFlash).setOnClickListener {
            toggleFlash()
        }

    }


    private fun toggleFlash() {
        val camera = camera ?: return

        if (camera.cameraInfo.hasFlashUnit()) {
            isFlashOn = !isFlashOn
            camera.cameraControl.enableTorch(isFlashOn)

            // Меняем иконку кнопки
            val flashButton = view?.findViewById<ImageButton>(R.id.btnFlash)
            if (isFlashOn) {
                flashButton?.setImageResource(R.drawable.ic_flash_on)
            } else {
                flashButton?.setImageResource(R.drawable.ic_flash_off)
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Настраиваем Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Настраиваем ImageCapture (для съёмки фото)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Выбираем заднюю камеру
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Отвязываем все предыдущие привязки
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                this.camera = camera

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Создаём временный файл для фото
        val photoFile = File(
            requireContext().externalMediaDirs.first(),
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Toast.makeText(requireContext(), "Фото сохранено", Toast.LENGTH_SHORT).show()

                    // TODO: передать фото обратно в ImageProcessingFragment
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка при съёмке", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 0) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // Разрешение дали - запускаем камеру
                startCamera()
            } else {
                // Не дали - показываем сообщение и возвращаемся
                Toast.makeText(requireContext(), "Нужно разрешение на камеру", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigateUp()
            }
        }
    }

    //проверка разрешений
    private fun hasRequiredPermissions(): Boolean {
        return CAMERAX_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(
                requireActivity().applicationContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    //массив разрешений необходимых
    companion object {
        private val CAMERAX_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
        )
    }
}