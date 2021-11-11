@file:Suppress("unused")

package uz.gita.demoplayer.utils.extensions

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import uz.gita.demoplayer.R

fun ImageView.loadImage(data: Bitmap) {
    Glide.with(this).load(data).centerCrop().into(this)
}

fun ImageView.loadImage(data: Uri?) {
    Glide.with(this).load(data).centerCrop().placeholder(R.drawable.ic_music).into(this)
}