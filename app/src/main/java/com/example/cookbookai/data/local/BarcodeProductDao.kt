package com.example.cookbookai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.cookbookai.data.model.BarcodeProduct

@Dao
interface BarcodeProductDao {

    @Query("SELECT * FROM barcode_products")
    suspend fun getAll(): List<BarcodeProduct>

    @Query("SELECT * FROM barcode_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): BarcodeProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: BarcodeProduct)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<BarcodeProduct>)
}
