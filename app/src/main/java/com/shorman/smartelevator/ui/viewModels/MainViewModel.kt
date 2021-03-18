package com.shorman.smartelevator.ui.viewModels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel:ViewModel() {

    //live data to save network status
    var isNetworkAvailable = MutableLiveData(true)

}