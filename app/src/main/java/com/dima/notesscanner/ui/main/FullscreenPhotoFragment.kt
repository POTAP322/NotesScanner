package com.dima.notesscanner.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.dima.notesscanner.R
import com.github.chrisbanes.photoview.PhotoView

class FullscreenPhotoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_fullscreen_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем путь к фото из аргументов
        val photoPath = arguments?.getString("photoPath")

        if (photoPath == null) {
            // Если путь не передан, закрываем фрагмент
            findNavController().navigateUp()
            return
        }

        // Находим PhotoView (или ImageView)
        val photoView = view.findViewById<PhotoView>(R.id.photoView)

        // Загружаем фото
        Glide.with(this)
            .load(photoPath)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(photoView)

        // Закрыть по клику (опционально)
        photoView.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}