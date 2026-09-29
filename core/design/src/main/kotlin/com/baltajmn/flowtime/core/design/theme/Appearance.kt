package com.baltajmn.flowtime.core.design.theme

import androidx.annotation.StringRes
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DARK_MODE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DYNAMIC_COLOR
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DarkMode(@StringRes val label: Int) {
    SYSTEM(R.string.appearance_system),
    LIGHT(R.string.appearance_light),
    DARK(R.string.appearance_dark)
}

data class Appearance(
    val theme: AppTheme = AppTheme.Blue,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    /** Los colores del fondo de pantalla (Material You). Solo se aplican desde Android 12. */
    val dynamicColor: Boolean = false
) {
    fun isDark(systemDark: Boolean) = when (darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
}

/** El aspecto de la app, en un solo sitio: lo leen la actividad y los ajustes, y lo cambian los ajustes. */
class AppearanceRepository(private val dataProvider: DataProvider) {

    private val _appearance = MutableStateFlow(read())
    val appearance: StateFlow<Appearance> = _appearance.asStateFlow()

    fun setTheme(theme: AppTheme) {
        dataProvider.setString(THEME_COLOR, theme.name)
        _appearance.update { it.copy(theme = theme) }
    }

    fun setDarkMode(mode: DarkMode) {
        dataProvider.setString(DARK_MODE, mode.name)
        _appearance.update { it.copy(darkMode = mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        dataProvider.setBoolean(DYNAMIC_COLOR, enabled)
        _appearance.update { it.copy(dynamicColor = enabled) }
    }

    private fun read() = Appearance(
        theme = AppTheme.entries.firstOrNull { it.name == dataProvider.getString(THEME_COLOR) } ?: AppTheme.Blue,
        darkMode = DarkMode.entries.firstOrNull { it.name == dataProvider.getString(DARK_MODE) } ?: DarkMode.SYSTEM,
        dynamicColor = dataProvider.getBoolean(DYNAMIC_COLOR, false)
    )
}
