package uz.gita.demoplayer.utils.extensions

import androidx.fragment.app.Fragment
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions


fun Fragment.checkPermission(permission: String, onGranted: () -> Unit) {
    Permissions.check(requireContext(), permission, null, object : PermissionHandler() {
        override fun onGranted() {
            onGranted()
        }
    })
}