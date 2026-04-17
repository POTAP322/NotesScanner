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

    // Временное хранилище для фото во время съёмки
    private val tempPhotos = mutableListOf<File>()

    fun addTempPhoto(photoFile: File) {
        tempPhotos.add(photoFile)
        // Обновляем capturedPhotos для отображения в камере (если нужно)
        _capturedPhotos.value = tempPhotos.toList()
    }

    fun commitPhotos() {
        // При нажатии "Готово" переносим временные фото в постоянный список
        val currentAll = _allPhotos.value?.toMutableList() ?: mutableListOf()
        currentAll.addAll(tempPhotos)
        _allPhotos.value = currentAll
        tempPhotos.clear()
        _capturedPhotos.value = emptyList()
    }

    fun cancelTempPhotos() {
        // При нажатии "Назад" удаляем временные фото
        tempPhotos.clear()
        _capturedPhotos.value = emptyList()
    }

    fun removePhoto(photoFile: File) {
        val currentAll = _allPhotos.value?.toMutableList() ?: return
        currentAll.remove(photoFile)
        _allPhotos.value = currentAll
    }

    fun clearPhotos() {
        _capturedPhotos.value = emptyList()
    }

    fun updatePhotosOrder(newOrder: List<File>) {
        _allPhotos.value = newOrder
    }
    fun clearAllPhotos() {
        _allPhotos.value = emptyList()
        _capturedPhotos.value = emptyList()
    }
    fun notifyPhotoChanged(updatedFile: File) {
        val current = _allPhotos.value?.toMutableList() ?: return
        // Обновляем список, чтобы триггерить observer
        _allPhotos.value = current
    }
}