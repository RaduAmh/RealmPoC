package com.example.realmpoc

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.where

class SurgeriesViewModel : ViewModel() {
    private var surgeries: RealmResults<Surgery>? = null
    private var surgeriesData: MutableLiveData<RealmResults<Surgery>>? = null

    fun getSurgeries(realm: Realm): LiveData<RealmResults<Surgery>> {
        if (surgeriesData == null){
            surgeriesData = MutableLiveData<RealmResults<Surgery>>()
            loadSurgeries(realm)
        }

        return surgeriesData as MutableLiveData<RealmResults<Surgery>>
    }

    private fun loadSurgeries(realm: Realm) {
        surgeries = realm.where<Surgery>().findAllAsync()
        surgeries?.addChangeListener{ collection -> surgeriesData?.postValue(collection)}
    }
}