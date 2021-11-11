package uz.gita.demoplayer.service

import androidx.lifecycle.MutableLiveData
import uz.gita.demoplayer.data.MusicState

object EventBus {
    val musicStateLiveData = MutableLiveData<MusicState>()
    val currentValueLiveData = MutableLiveData<Int>()
}