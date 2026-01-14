package cz.ash.mobilniapplikace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_posts")
data class FavoritePostEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val body: String
)

