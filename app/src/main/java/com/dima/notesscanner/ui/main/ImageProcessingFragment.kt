package com.dima.notesscanner.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dima.notesscanner.R
import android.widget.Button
import androidx.navigation.fragment.findNavController

class ImageProcessingFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnToPreview).setOnClickListener(){
            findNavController().navigate(R.id.action_image_processing_to_preview)
        }
        view.findViewById<Button>(R.id.btnToEditing).setOnClickListener(){
            findNavController().navigate(R.id.action_image_processing_to_image_editing)
        }
        view.findViewById<Button>(R.id.btnToCamera).setOnClickListener {
            findNavController().navigate(R.id.action_image_processing_to_camera)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_processing, container, false)
    }

}