package com.baltajmn.flowtime

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.ui.FlowTimeApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
    private val sessionNotification: SessionNotification by inject()
    private val theme: MutableState<AppTheme> = mutableStateOf(AppTheme.Blue)
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

        theme.value = viewModel.getAppTheme()
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
            FlowTimeApp(
                appTheme = theme.value,
                showOnBoard = viewModel.getShowOnBoard(),
                showRating = viewModel.getShowRating(),
                rememberShowRating = viewModel.getRememberShowRating(),
                showSound = showSound.value,
                onSoundChange = { it: Boolean -> showSound.value = it },
                onThemeChanged = { it: AppTheme -> theme.value = it },
                onShowRatingChanged = { it: Boolean -> viewModel.setShowRating(it) },
                onSupportDeveloperClick = { initiatePurchase() },
                onRememberShowRating = { it: Boolean -> viewModel.setRememberShowRating(it) }
            )
        }
    }

    // Desde Android 14 se puede descartar; vuelve al abrir la app, y también justo después de dar
    // el permiso, que no para la actividad.
    override fun onResume() {
        super.onResume()
        sessionNotification.update()
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
}