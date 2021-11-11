@file:Suppress("unused")

package uz.gita.demoplayer.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class BooleanPreference(
    private val pref: SharedPreferences,
    private val defValue: Boolean = false
) : ReadWriteProperty<Any, Boolean> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        pref.getBoolean(property.name, defValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) =
        pref.edit { putBoolean(property.name, value).apply() }
}

class IntPreference(
    private val pref: SharedPreferences,
    private val defValue: Int = -1
) : ReadWriteProperty<Any, Int> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        pref.getInt(property.name, defValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Int) =
        pref.edit { putInt(property.name, value).apply() }
}

class LongPreference(
    private val pref: SharedPreferences,
    private val defValue: Long = -1L
) : ReadWriteProperty<Any, Long> {
    override fun getValue(thisRef: Any, property: KProperty<*>) =
        pref.getLong(property.name, defValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Long) =
        pref.edit { putLong(property.name, value).apply() }
}

class StringPreference(
    private val pref: SharedPreferences,
    private val defValue: String = ""
) : ReadWriteProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String =
        pref.getString(property.name, defValue) ?: ""

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String) =
        pref.edit { putString(property.name, value).apply() }
}

class DoublePreference(
    private val pref: SharedPreferences,
    private val defValue: Double = 0.0
) : ReadWriteProperty<Any, Double> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Double =
        java.lang.Double.longBitsToDouble(
            pref.getLong(
                property.name,
                java.lang.Double.doubleToLongBits(defValue)
            )
        )

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Double) =
        pref.edit().putLong(property.name, java.lang.Double.doubleToRawLongBits(value)).apply()
}
