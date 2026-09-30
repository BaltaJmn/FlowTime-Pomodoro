package com.baltajmn.flowtime.core.persistence.sharedpreferences

import platform.Foundation.NSUserDefaults

/** Los ajustes en el iPhone, con las mismas claves que en Android. */
class UserDefaultsProvider(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults
) : DataProvider {

    private val SharedPreferencesItem.key get() = name.lowercase()

    override fun getString(key: SharedPreferencesItem): String? = defaults.stringForKey(key.key)

    override fun setString(key: SharedPreferencesItem, value: String) {
        defaults.setObject(value, forKey = key.key)
    }

    // boolForKey y floatForKey dan 0 si no hay nada guardado: el valor por defecto hay que ponerlo aquí.
    override fun getBoolean(key: SharedPreferencesItem, defValue: Boolean): Boolean =
        if (defaults.objectForKey(key.key) == null) defValue else defaults.boolForKey(key.key)

    override fun setBoolean(key: SharedPreferencesItem, value: Boolean) {
        defaults.setBool(value, forKey = key.key)
    }

    override fun getLong(key: SharedPreferencesItem): Long = defaults.integerForKey(key.key)

    override fun setLong(key: SharedPreferencesItem, value: Long) {
        defaults.setInteger(value, forKey = key.key)
    }

    override fun getFloat(key: String, defValue: Float): Float =
        if (defaults.objectForKey(key) == null) defValue else defaults.floatForKey(key)

    override fun setFloat(key: String, value: Float) {
        defaults.setFloat(value, forKey = key)
    }

    /** En el iPhone no hay minutos de antes de las sesiones: la app nació con ellas. */
    override fun getStudyTimeMap(): Map<String, Long> = emptyMap()

    override fun setCheckValue(key: SharedPreferencesItem, value: Boolean) = setBoolean(key, value)

    override fun getCheckValue(key: SharedPreferencesItem): Boolean = getBoolean(key, true)
}
