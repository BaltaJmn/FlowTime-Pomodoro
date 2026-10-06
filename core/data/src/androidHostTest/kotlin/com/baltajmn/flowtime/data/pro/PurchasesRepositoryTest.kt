package com.baltajmn.flowtime.data.pro

import android.app.Activity
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.IS_PRO
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PurchasesRepositoryTest {

    /** La tienda falsa: devuelve [owned], o lanza como sin conexión si [offline]. */
    private class FakeStore : Store {
        var owned = emptySet<String>()
        var offline = false
        var cancel = false
        var listener: ((Set<String>) -> Unit)? = null

        private fun reply() = if (offline) throw IOException("sin conexión") else owned

        override suspend fun entitlements() = reply()
        override suspend fun catalog() = if (offline) throw IOException() else Catalog(PRO, listOf(TIP))
        override suspend fun purchase(host: PurchaseHost, item: CatalogItem): Set<String>? {
            if (cancel) return null
            owned = owned + if (item == PRO) Entitlements.PRO else Entitlements.SUPPORTER
            return reply()
        }

        override suspend fun restore() = reply()
        override fun onChange(listener: (Set<String>) -> Unit) {
            this.listener = listener
        }
    }

    private val prefs = FakeDataProvider()
    private val store = FakeStore()
    private val activity = mockk<Activity>()

    @Test
    fun `sin conexion se usa lo que se guardo`() = runTest {
        prefs.setBoolean(IS_PRO, true)
        store.offline = true
        val purchases = DefaultPurchasesRepository(store, prefs)

        purchases.refresh()

        assertTrue(purchases.isPro.value)
        assertNull(purchases.catalog())
    }

    @Test
    fun `un error de red no quita Pro`() = runTest {
        val purchases = DefaultPurchasesRepository(store, prefs)
        assertEquals(PurchaseOutcome.Success, purchases.purchase(activity, PRO))

        store.offline = true
        purchases.refresh()

        assertTrue(purchases.isPro.value)
        assertTrue(DefaultPurchasesRepository(store, prefs).isPro.value)
    }

    @Test
    fun `un reembolso quita Pro, al refrescar o cuando avisa la tienda`() = runTest {
        val purchases = DefaultPurchasesRepository(store, prefs)
        purchases.purchase(activity, PRO)

        store.owned = emptySet()
        purchases.refresh()
        assertFalse(purchases.isPro.value)

        store.listener!!(setOf(Entitlements.PRO))
        assertTrue(purchases.isPro.value)
        store.listener!!(emptySet())
        assertFalse(purchases.isPro.value)
    }

    @Test
    fun `la insignia de supporter no se pierde`() = runTest {
        val purchases = DefaultPurchasesRepository(store, prefs)
        purchases.purchase(activity, TIP)
        assertTrue(purchases.isSupporter.value)
        assertFalse(purchases.isPro.value)

        // Reinstalada sin la copia de RevenueCat: un usuario anónimo nuevo, sin propinas.
        store.owned = emptySet()
        purchases.refresh()

        assertTrue(purchases.isSupporter.value)
    }

    @Test
    fun `restaurar sin compras devuelve false`() = runTest {
        val purchases = DefaultPurchasesRepository(store, prefs)
        assertFalse(purchases.restore())

        store.owned = setOf(Entitlements.PRO)
        assertTrue(purchases.restore())
        assertTrue(purchases.isPro.value)
    }

    @Test
    fun `cancelar no cambia nada`() = runTest {
        store.cancel = true
        val purchases = DefaultPurchasesRepository(store, prefs)

        assertEquals(PurchaseOutcome.Cancelled, purchases.purchase(activity, PRO))
        assertFalse(purchases.isPro.value)
    }

    @Test
    fun `sin clave de RevenueCat todo es gratis`() = runTest {
        val purchases = DefaultPurchasesRepository(null, prefs)
        purchases.refresh()

        assertFalse(purchases.isPro.value)
        assertNull(purchases.catalog())
        assertEquals(PurchaseOutcome.Failed, purchases.purchase(activity, PRO))
        assertFalse(purchases.restore())
    }

    private companion object {
        val PRO = CatalogItem(Products.PRO, "$1.99")
        val TIP = CatalogItem(Products.TIPS.first(), "$1.99")
    }
}
