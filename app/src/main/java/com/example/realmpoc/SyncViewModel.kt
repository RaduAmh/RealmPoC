package com.example.realmpoc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SyncViewModel : ViewModel() {
    private val _status = MutableLiveData("")
    val status: LiveData<String> = _status

    fun send(value: String) {
        _status.postValue(value)
    }
}