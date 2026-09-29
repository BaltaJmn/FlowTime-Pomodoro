package com.baltajmn.flowtime

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.review.calmMoments
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.goal.GoalWatcher
import com.baltajmn.flowtime.review.ReviewPrompter
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.ui.FlowTimeApp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
    private val sessionNotification: SessionNotification by inject()
    private val appearanceRepository: AppearanceRepository by inject()
    private val goalWatcher: GoalWatcher by inject()
    private val engine: FocusEngine by inject()
    private val reviewPrompter: ReviewPrompter by inject()
    private val showSound: MutableState<Boolean> = mutableStateOf(true)

    private val queryProductDetailsParams =
        QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId("support_developer")
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach(::consume)
                }

                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Log.d("MainActivity", "Purchase canceled by user")
                }

                else -> {
                    Log.e("MainActivity", "Purchase failed: ${billingResult.debugMessage}")
                }
            }
        }

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.enableEdgeToEdge()

        showSound.value = viewModel.getShowSound()

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .enableAutoServiceReconnection()
            .build()

        connectBillingClient()

        setContent {
            val appearance by appearanceRepository.appearance.collectAsStateWithLifecycle()
            val celebration by goalWatcher.celebration.collectAsStateWithLifecycle()
            val dark = appearance.isDark(isSystemInDarkTheme())
            // Los iconos de las barras del sistema siguen al tema de la app, no al del sistema: con el
            // modo oscuro forzado en un móvil claro, se quedaban oscuros sobre fondo oscuro.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose {}
            }
            FlowTimeApp(
                appearance = appearance,
                showSound = showSound.value,
                onSoundChange = { it: Boolean -> showSound.value = it },
                onSupportDeveloperClick = { initiatePurchase() },
                celebration = celebration,
                onCelebrationShown = goalWatcher::onShown
            )
        }

        // La valoración, solo con la app a la vista y en un momento tranquilo: al terminar una sesión
        // o al cerrar la celebración del objetivo. Nunca al abrir la app ni con una sesión en marcha.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                calmMoments(
                    sessionRunning = engine.state.map { it.isActive },
                    celebrating = goalWatcher.celebration.map { it != null }
                ).collect { reviewPrompter.askIfDue(this@MainActivity) }
            }
        }
    }

    // Desde Android 14 se puede descartar; vuelve al abrir la app, y también justo después de dar
    // el permiso, que no para la actividad.
    override fun onResume() {
        super.onResume()
        sessionNotification.update()
        sessionNotification.dismissAlert()
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    consumePendingPurchases()
                } else {
                    Log.e(
                        "MainActivity",
                        "Billing client setup failed: ${billingResult.debugMessage}"
                    )
                }
            }

            // enableAutoServiceReconnection() reconecta sola en la siguiente llamada.
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    private fun queryProductDetails() {
        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult, result ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    viewModel.setProductDetailsList(result.productDetailsList)
                }

                else -> {
                    Log.e(
                        "MainActivity",
                        "Product details query failed: ${billingResult.debugMessage}"
                    )
                }
            }
        }
    }

    // Compras que terminaron con la app cerrada, o pendientes que se pagaron despues.
    private fun consumePendingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::consume)
            }
        }
    }

    // Play reembolsa cualquier compra que no se reconozca en tres dias. Consumir la donacion la
    // reconoce y deja volver a donar.
    private fun consume(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        billingClient.consumeAsync(
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        ) { billingResult, _ ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e("MainActivity", "Consume failed: ${billingResult.debugMessage}")
            }
        }
    }

    private fun initiatePurchase() {
        viewModel
            .getProductDetailsList()
            .takeIf { it.isNotEmpty() }
            ?.let {
                val billingFlowParams = BillingFlowParams
                    .newBuilder()
                    .setProductDetailsParamsList(
                        it.map {
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(it)
                                .build()
                        }
                    )
                    .build()
                billingClient.launchBillingFlow(this, billingFlowParams)
            } ?: launchAlert()
    }

    private fun launchAlert() {
        AlertDialog
            .Builder(this)
            .setTitle(applicationContext.getString(R.string.alert_google_play))
            .setMessage(applicationContext.getString(R.string.alert_google_play_desc))
            .setPositiveButton(applicationContext.getString(R.string.dialog_confirm)) { dialog, which ->
                connectBillingClient()
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private companion object {
        // Los mismos velos que pone enableEdgeToEdge() por defecto en la barra de navegación de botones.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
