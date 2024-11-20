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
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.ui.FlowTimeApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
    private val theme: MutableState<AppTheme> = mutableStateOf(AppTheme.Blue)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(FLAG_KEEP_SCREEN_ON)

        theme.value = viewModel.getAppTheme()

        setContent {

            val purchasesUpdatedListener =
                PurchasesUpdatedListener { billingResult, purchases ->
                    // To be implemented in a later section.
                }

            var billingClient = BillingClient.newBuilder(this)
                .setListener(purchasesUpdatedListener)
                .build()

            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            Log.d("MainActivity", "onBillingSetupFinished: ")
                        }

                        else -> {}
                    }
                }

                override fun onBillingServiceDisconnected() {

                }
            })

            val queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId("product_id_example")
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        )
                    )
                    .build()

            billingClient
                .queryProductDetailsAsync(queryProductDetailsParams) { billingResult,
                                                                       productDetailsList ->
                    when (billingResult.responseCode) {
                        BillingClient.BillingResponseCode.OK -> {
                            Log.d("MainActivity", "onBillingSetupFinished: ")
                        }

                        else -> {}
                    }
                }

            FlowTimeApp(
                appTheme = theme.value,
                showRating = viewModel.getShowRating(),
                rememberShowRating = viewModel.getRememberShowRating(),
                onThemeChanged = { it: AppTheme -> theme.value = it },
                onShowRatingChanged = { it: Boolean -> viewModel.setShowRating(it) },
                onRememberShowRating = { it: Boolean -> viewModel.setRememberShowRating(it) }
            )
        }
    }
}