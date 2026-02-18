package com.dima.notesscanner.ui.main
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.dima.notesscanner.MainActivity
import com.dima.notesscanner.R

class CameraFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        if(!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                CAMERAX_PERMISSIONS,
                0
            )
        } else {


        }

        view.findViewById<Button>(R.id.btnBack2).setOnClickListener {
            findNavController().navigate(R.id.action_camera_to_image_processing)
        }
    }


    //проверка разрешений
    private fun hasRequiredPermissions() : Boolean{
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