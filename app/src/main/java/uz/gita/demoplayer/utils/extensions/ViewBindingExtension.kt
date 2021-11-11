package uz.gita.demoplayer.utils.extensions

import androidx.viewbinding.ViewBinding

fun <T : ViewBinding> T.scope(block: T.() -> Unit) {
    block(this)
}