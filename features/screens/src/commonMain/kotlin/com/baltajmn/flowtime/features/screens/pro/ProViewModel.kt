package com.baltajmn.flowtime.features.screens.pro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.data.pro.CatalogItem
import com.baltajmn.flowtime.data.pro.PurchaseOutcome
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.baltajmn.flowtime.data.pro.PurchaseHost

data class ProUiState(
    val loading: Boolean = true,
    /** Pro con el precio de la tienda para el país del usuario, o null si la tienda no responde. */
    val product: CatalogItem? = null,
    val busy: Boolean = false,
    val failed: Boolean = false,
    val nothingToRestore: Boolean = false,
    /** Con Pro, la pantalla solo da las gracias: recién comprado, restaurado o de antes. */
    val isPro: Boolean = false
)

/** La pantalla de Pro (#57): el precio sale de la tienda y Pro, de [PurchasesRepository.isPro]. */
class ProViewModel(private val purchases: PurchasesRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProUiState(isPro = purchases.isPro.value))
    val state: StateFlow<ProUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            purchases.isPro.collect { pro -> _state.update { it.copy(isPro = pro) } }
        }
    }

    /** Cada vez que se abre, y con "Reintentar": si la tienda no respondió, puede que ahora sí. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false, nothingToRestore = false) }
            val product = purchases.catalog()?.pro
            _state.update { it.copy(loading = false, product = product) }
        }
    }

    fun buy(host: PurchaseHost) {
        val product = _state.value.product ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, failed = false, nothingToRestore = false) }
            val outcome = purchases.purchase(host, product)
            _state.update { it.copy(busy = false, failed = outcome == PurchaseOutcome.Failed) }
        }
    }

    fun restore() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, failed = false, nothingToRestore = false) }
            purchases.restore()
            _state.update { it.copy(busy = false, nothingToRestore = !purchases.isPro.value) }
        }
    }
}
