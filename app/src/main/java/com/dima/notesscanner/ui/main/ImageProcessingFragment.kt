package com.dima.notesscanner.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dima.notesscanner.R
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import java.io.File
import com.dima.notesscanner.viewmodel.SharedGalleryViewModel
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ImageProcessingFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotosAdapter
    private val photosList = mutableListOf<File>() // Список фото

    private val sharedGalleryViewModel: SharedGalleryViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_processing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView(view)

        sharedGalleryViewModel.allPhotos.observe(viewLifecycleOwner) { allPhotos ->
            photosList.clear()
            photosList.addAll(allPhotos)
            adapter.notifyDataSetChanged()
        }

        sharedGalleryViewModel.capturedPhotos.observe(viewLifecycleOwner) { newPhotos ->
            if (newPhotos.isNotEmpty()) {
                sharedGalleryViewModel.clearPhotos()
            }
        }

        setupButtons(view)
    }

    private fun setupRecyclerView(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PhotosAdapter(
            photos = photosList,
            onItemClick = { photoFile ->
                val bundle = Bundle().apply {
                    putString("photoPath", photoFile.absolutePath)
                }
                findNavController().navigate(
                    R.id.action_image_processing_to_image_editing,
                    bundle
                )
            },
            onItemLongClick = { photoFile ->
                // Показываем диалог удаления
                showDeleteDialog(photoFile)
            }
        )

        recyclerView.adapter = adapter
    }

    private fun setupButtons(view: View) {
        view.findViewById<ImageButton>(R.id.btnToCamera).setOnClickListener {
            findNavController().navigate(R.id.action_image_processing_to_camera)
        }

        view.findViewById<ImageButton>(R.id.btnToPreview).setOnClickListener {
            if (photosList.isEmpty()) {
                Toast.makeText(requireContext(), "Сначала добавьте фото", Toast.LENGTH_SHORT).show()
            } else {
                findNavController().navigate(R.id.action_image_processing_to_preview)
            }
        }
    }

    private fun showDeleteDialog(photoFile: File) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить фото")
            .setMessage("Вы уверены, что хотите удалить это фото?")
            .setPositiveButton("Удалить") { _, _ ->
                // Удаляем из ViewModel (обновит allPhotos и вызовет observer)
                sharedGalleryViewModel.removePhoto(photoFile)
                // Удаляем файл
                photoFile.delete()
                Toast.makeText(requireContext(), "Фото удалено", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}