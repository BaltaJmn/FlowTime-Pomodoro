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
    /** El vitalicio con el precio de la tienda para el país del usuario, o null si la tienda no responde. */
    val product: CatalogItem? = null,
    /** El mensual, o null si la tienda no lo ofrece: entonces la pantalla es la de siempre. */
    val monthly: CatalogItem? = null,
    /** Lo que se compra al pulsar: el vitalicio, salvo que se haya elegido el mensual. */
    val monthlyPicked: Boolean = false,
    /** Tuvo un mensual y caducó. */
    val ended: Boolean = false,
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
        viewModelScope.launch {
            purchases.proEnded.collect { ended -> _state.update { it.copy(ended = ended) } }
        }
    }

    /** Cada vez que se abre, y con "Reintentar": si la tienda no respondió, puede que ahora sí. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, failed = false, nothingToRestore = false) }
            val catalog = purchases.catalog()
            _state.update {
                it.copy(loading = false, product = catalog?.pro ?: catalog?.proMonthly, monthly = catalog?.proMonthly.takeIf { catalog?.pro != null })
            }
        }
    }

    fun pick(monthly: Boolean) = _state.update { it.copy(monthlyPicked = monthly) }

    fun buy(host: PurchaseHost) {
        val current = _state.value
        val product = (if (current.monthlyPicked) current.monthly else current.product) ?: return
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
