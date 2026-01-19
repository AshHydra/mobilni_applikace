package cz.ash.mobilniapplikace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_sync")
data class CurrencySyncEntity(
    @PrimaryKey val vsCurrency: String,
    val lastFetchedAtMs: Long,
    val nextAllowedAtMs: Long
)

