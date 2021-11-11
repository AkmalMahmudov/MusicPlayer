package uz.gita.demoplayer.utils.extensions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

val ViewGroup.inflater get() = LayoutInflater.from(context)

fun ViewGroup.inflate(@LayoutRes layoutId:Int):View = inflater.inflate(layoutId, this, false)