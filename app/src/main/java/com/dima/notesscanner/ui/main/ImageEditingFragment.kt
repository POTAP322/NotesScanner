package com.dima.notesscanner.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.dima.notesscanner.R

class ImageEditingFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_editing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val photoPath = arguments?.getString("photoPath")
        val ivPhoto: ImageView = view.findViewById<ImageView>(R.id.mainImage)

        Glide.with(ivPhoto.context)
            .load(photoPath)
            .placeholder(R.drawable.ic_broken_image)
            .into(ivPhoto)

        setupButtons(view)
    }

    private fun setupButtons(view: View){
        view.findViewById<Button>(R.id.btnBack).setOnClickListener(){
            findNavController().navigate(R.id.action_image_editing_to_image_processing)
        }
    }


}