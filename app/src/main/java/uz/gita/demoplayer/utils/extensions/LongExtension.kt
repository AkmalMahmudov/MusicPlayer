package uz.gita.demoplayer.utils.extensions

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.*

val Long.time: String
    @SuppressLint("SimpleDateFormat")
    get() = SimpleDateFormat("mm:ss").format(Date(this))