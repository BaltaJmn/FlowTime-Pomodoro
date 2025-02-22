package com.baltajmn.flowtime

import android.os.Bundle
import android.util.Log
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.ui.FlowTimeApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
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
                    purchases?.forEach { purchase ->
                        Log.d("MainActivity", "Purchase successful: ${purchase.products}")
                    }
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        theme.value = viewModel.getAppTheme()
        showSound.value = viewModel.getShowSound()

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
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

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                } else {
                    Log.e(
                        "MainActivity",
                        "Billing client setup failed: ${billingResult.debugMessage}"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                connectBillingClient()
            }
        })
    }

    private fun queryProductDetails() {
        billingClient.queryProductDetailsAsync(
            queryProductDetailsParams
        ) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    viewModel.setProductDetailsList(productDetailsList)
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

    private fun initiatePurchase() {
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(viewModel.getProductDetailsList())
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(this, billingFlowParams)
    }
}