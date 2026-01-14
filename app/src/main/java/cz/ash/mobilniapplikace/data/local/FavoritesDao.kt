package cz.ash.mobilniapplikace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorite_posts ORDER BY id ASC")
    fun observeAll(): Flow<List<FavoritePostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoritePostEntity)

    @Query("DELETE FROM favorite_posts WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_posts WHERE id = :id)")
    fun observeIsFavorite(id: Int): Flow<Boolean>
}

