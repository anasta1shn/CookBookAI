package com.example.cookbookai.data.local

import android.content.Context
import android.util.Log
import com.example.cookbookai.data.model.BarcodeProduct
import com.example.cookbookai.data.remote.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.Json

object BarcodeProductDatabaseManager {

    private var products: List<BarcodeProduct> =
        emptyList()

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    fun loadDatabase(
        context: Context
    ) {

        try {

            val jsonString =
                openBarcodeDatabase(context)

            val baseProducts: List<BarcodeProduct> =
                json.decodeFromString(jsonString)

            products =
                baseProducts

            Log.d(
                "BarcodeDB",
                "Loaded products: ${products.size}"
            )

        } catch (e: Exception) {

            Log.e(
                "BarcodeDB",
                "Load error: ${e.message}"
            )

            products =
                emptyList()
        }
    }

    suspend fun loadSavedProducts(
        context: Context
    ) {

        try {

            val localProducts =
                AppDatabase.get(context)
                    .barcodeProductDao()
                    .getAll()

            products =
                mergeProducts(
                    products,
                    localProducts
                )

            Log.d(
                "BarcodeDB",
                "Loaded saved products: ${localProducts.size}"
            )

        } catch (e: Exception) {

            Log.e(
                "BarcodeDB",
                "Saved products load error: ${e.message}"
            )
        }
    }

    suspend fun syncFromSupabase(
        context: Context
    ) {

        try {

            val remoteProducts =
                SupabaseManager.client
                    .postgrest["barcode_products"]
                    .select()
                    .decodeList<BarcodeProduct>()

            if (remoteProducts.isNotEmpty()) {

                AppDatabase.get(context)
                    .barcodeProductDao()
                    .insertAll(remoteProducts)

                products =
                    mergeProducts(
                        products,
                        remoteProducts
                    )
            }

            Log.d(
                "BarcodeDB",
                "Synced remote products: ${remoteProducts.size}"
            )

        } catch (e: Exception) {

            Log.e(
                "BarcodeDB",
                "Supabase sync error: ${e.message}"
            )
        }
    }

    fun findByBarcode(
        barcode: String
    ): BarcodeProduct? {

        val scanned =
            normalizeBarcode(barcode)

        Log.d(
            "BarcodeDB",
            "Scanned barcode raw: $barcode"
        )

        Log.d(
            "BarcodeDB",
            "Scanned barcode normalized: $scanned"
        )

        Log.d(
            "BarcodeDB",
            "Products count: ${products.size}"
        )

        val found =
            products.find { product ->

                val productBarcode =
                    normalizeBarcode(product.barcode)

                productBarcode == scanned ||
                        productBarcode.removePrefix("0") == scanned.removePrefix("0")
            }

        Log.d(
            "BarcodeDB",
            "Found product: ${found?.name}"
        )

        return found
    }

    fun getAllProducts(): List<BarcodeProduct> {
        return products
    }

    suspend fun addProduct(
        context: Context,
        product: BarcodeProduct
    ) {

        val productWithUser =
            product.copy(
                userId =
                    SupabaseManager.client
                        .auth
                        .currentUserOrNull()
                        ?.id
            )

        AppDatabase.get(context)
            .barcodeProductDao()
            .insert(productWithUser)

        products =
            mergeProducts(
                products,
                listOf(productWithUser)
            )

        try {

            SupabaseManager.client
                .postgrest["barcode_products"]
                .insert(productWithUser)

        } catch (e: Exception) {

            Log.e(
                "BarcodeDB",
                "Supabase insert error: ${e.message}"
            )
        }
    }

    private fun mergeProducts(
        first: List<BarcodeProduct>,
        second: List<BarcodeProduct>
    ): List<BarcodeProduct> {

        return (first + second)
            .associateBy {
                normalizeBarcode(
                    it.barcode
                )
            }
            .values
            .toList()
    }

    private fun openBarcodeDatabase(
        context: Context
    ): String {

        val fileNames =
            listOf(
                "barcode_products.json",
                "barcode-product.json"
            )

        for (fileName in fileNames) {

            try {

                return context.assets
                    .open(fileName)
                    .bufferedReader()
                    .use { it.readText() }

            } catch (_: Exception) {
            }
        }

        error("Barcode database file not found")
    }

    private fun normalizeBarcode(
        value: String
    ): String {

        return value
            .trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("\n", "")
            .replace("\r", "")
    }
}
