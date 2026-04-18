package com.dima.notesscanner

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            // Загружаем OpenCV при старте приложения
            if (OpenCVLoader.initLocal()) {
                Log.d("OpenCV", "OpenCV loaded successfully")
            } else {
                Log.e("OpenCV", "OpenCV initialization failed!")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Настройка навигации
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
    }
}
