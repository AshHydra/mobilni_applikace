package cz.ash.mobilniapplikace.data.local

import androidx.room.Entity

@Entity(
    tableName = "market_coins",
    primaryKeys = ["vsCurrency", "id"]
)
data class MarketCoinEntity(
    val vsCurrency: String,
    val id: String,
    val symbol: String,
    val name: String,
    val imageUrl: String?,
    val price: Double?,
    val change24hPct: Double?,
    val marketCap: Double?,
    val updatedAtMs: Long
)

