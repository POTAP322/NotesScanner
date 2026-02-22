package com.dima.notesscanner.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class SharedGalleryViewModel : ViewModel() {

    // Для новых фото из камеры
    private val _capturedPhotos = MutableLiveData<List<File>>(emptyList())
    val capturedPhotos: LiveData<List<File>> = _capturedPhotos

    // Для всех фото (постоянное хранилище)
    private val _allPhotos = MutableLiveData<List<File>>(emptyList())
    val allPhotos: LiveData<List<File>> = _allPhotos

    fun addPhoto(photoFile: File) {
        // Добавляем во временный список (для камеры)
        val currentCaptured = _capturedPhotos.value?.toMutableList() ?: mutableListOf()
        currentCaptured.add(photoFile)
        _capturedPhotos.value = currentCaptured

        // Добавляем в постоянный список
        val currentAll = _allPhotos.value?.toMutableList() ?: mutableListOf()
        currentAll.add(photoFile)
        _allPhotos.value = currentAll
    }

    fun removePhoto(photoFile: File) {
        val currentAll = _allPhotos.value?.toMutableList() ?: return
        currentAll.remove(photoFile)
        _allPhotos.value = currentAll
    }

    fun clearPhotos() {
        _capturedPhotos.value = emptyList()
    }
}