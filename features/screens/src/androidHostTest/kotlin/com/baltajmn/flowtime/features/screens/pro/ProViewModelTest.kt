package com.baltajmn.flowtime.features.screens.pro

import android.app.Activity
import com.baltajmn.flowtime.data.pro.PurchaseOutcome
import com.baltajmn.flowtime.features.screens.fakes.FakePurchases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// Con Robolectric solo por la Activity que pide la compra.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProViewModelTest {

    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).get()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `el precio sale de la tienda, y al comprar da las gracias`() {
        val viewModel = ProViewModel(FakePurchases())
        viewModel.load()
        assertEquals("$1.99", viewModel.state.value.product?.price)
        assertFalse(viewModel.state.value.isPro)

        viewModel.buy(activity)

        assertTrue(viewModel.state.value.isPro)
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun `sin conexion no hay producto, y al reintentar con conexion si`() {
        val purchases = FakePurchases(online = false)
        val viewModel = ProViewModel(purchases)
        viewModel.load()
        assertNull(viewModel.state.value.product)
        assertFalse(viewModel.state.value.loading)

        // Sin producto, comprar no hace nada.
        viewModel.buy(activity)
        assertFalse(viewModel.state.value.isPro)

        purchases.online = true
        viewModel.load()
        assertEquals("$1.99", viewModel.state.value.product?.price)
    }

    @Test
    fun `una compra que falla lo dice, y una cancelada no`() {
        val purchases = FakePurchases(outcome = PurchaseOutcome.Failed)
        val viewModel = ProViewModel(purchases).apply { load() }

        viewModel.buy(activity)
        assertTrue(viewModel.state.value.failed)

        purchases.outcome = PurchaseOutcome.Cancelled
        viewModel.buy(activity)
        assertFalse(viewModel.state.value.failed)
        assertFalse(viewModel.state.value.isPro)
    }

    @Test
    fun `restaurar sin Pro lo dice, y con Pro da las gracias`() {
        val purchases = FakePurchases()
        val viewModel = ProViewModel(purchases).apply { load() }

        viewModel.restore()
        assertTrue(viewModel.state.value.nothingToRestore)

        purchases.restores = true
        viewModel.restore()
        assertFalse(viewModel.state.value.nothingToRestore)
        assertTrue(viewModel.state.value.isPro)
    }

    @Test
    fun `al volver a abrirla no arrastra el error de la vez anterior`() {
        val viewModel = ProViewModel(FakePurchases()).apply { load() }
        viewModel.restore()
        assertTrue(viewModel.state.value.nothingToRestore)

        viewModel.load()

        assertFalse(viewModel.state.value.nothingToRestore)
    }
}
