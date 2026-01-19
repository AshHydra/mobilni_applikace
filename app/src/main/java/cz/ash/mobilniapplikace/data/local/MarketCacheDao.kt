package cz.ash.mobilniapplikace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MarketCacheDao {
    @Query("SELECT * FROM market_coins WHERE vsCurrency = :vsCurrency ORDER BY marketCap DESC")
    suspend fun getAllForCurrency(vsCurrency: String): List<MarketCoinEntity>

    @Query("SELECT * FROM market_coins WHERE vsCurrency = :vsCurrency AND id IN (:ids)")
    suspend fun getByIds(vsCurrency: String, ids: List<String>): List<MarketCoinEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MarketCoinEntity>)

    @Query("DELETE FROM market_coins WHERE vsCurrency = :vsCurrency")
    suspend fun deleteForCurrency(vsCurrency: String)

    // Rate-limit metadata
    @Query("SELECT * FROM currency_sync WHERE vsCurrency = :vsCurrency LIMIT 1")
    suspend fun getSync(vsCurrency: String): CurrencySyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSync(sync: CurrencySyncEntity)
}

