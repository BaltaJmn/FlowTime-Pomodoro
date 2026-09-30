package com.baltajmn.flowtime.features.screens.support

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.pro.CatalogItem
import com.baltajmn.flowtime.data.pro.PurchaseOutcome
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupportState(
    val loading: Boolean = true,
    /** Vacía si la tienda no ha respondido. */
    val tips: List<CatalogItem> = emptyList(),
    val buying: Boolean = false,
    val failed: Boolean = false,
    val thanked: Boolean = false
)

/** Las propinas (#58): no desbloquean Pro. Dan la insignia y el tema Supporter. */
class SupportViewModel(
    private val purchases: PurchasesRepository,
    private val appearance: AppearanceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SupportState())
    val state: StateFlow<SupportState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false) }
            val tips = purchases.catalog()?.tips.orEmpty()
            _state.update { it.copy(loading = false, tips = tips) }
        }
    }

    fun tip(activity: Activity, item: CatalogItem) {
        viewModelScope.launch {
            _state.update { it.copy(buying = true, failed = false) }
            val outcome = purchases.purchase(activity, item)
            _state.update {
                it.copy(
                    buying = false,
                    thanked = outcome == PurchaseOutcome.Success,
                    failed = outcome == PurchaseOutcome.Failed
                )
            }
        }
    }

    fun useSupporterTheme() = appearance.setTheme(AppTheme.Supporter)

    /** Se puede dejar más de una propina: al volver a abrir, se empieza de nuevo. */
    fun onClosed() = _state.update { it.copy(thanked = false, failed = false) }
}
