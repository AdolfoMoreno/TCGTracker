package com.pokemontcg.tracker.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.pokemontcg.tracker.data.model.*

@Dao
interface SetDao {
    @Query("SELECT * FROM sets ORDER BY releaseDate DESC")
    fun getAllSets(): LiveData<List<PokemonSet>>

    @Query("SELECT * FROM sets ORDER BY releaseDate DESC")
    suspend fun getAllSetsSuspend(): List<PokemonSet>

    @Query("SELECT * FROM sets WHERE id = :setId")
    suspend fun getSetById(setId: String): PokemonSet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<PokemonSet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: PokemonSet)

    @Query("SELECT COUNT(*) FROM sets")
    suspend fun getSetCount(): Int
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE setId = :setId ORDER BY CAST(number AS INTEGER)")
    fun getCardsForSet(setId: String): LiveData<List<PokemonCard>>

    @Query("SELECT * FROM cards WHERE setId = :setId ORDER BY CAST(number AS INTEGER)")
    suspend fun getCardsForSetSuspend(setId: String): List<PokemonCard>

    @Query("SELECT * FROM cards WHERE id = :cardId")
    suspend fun getCardById(cardId: String): PokemonCard?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<PokemonCard>)

    @Query("SELECT COUNT(*) FROM cards WHERE setId = :setId")
    suspend fun getCardCountForSet(setId: String): Int

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getTotalCardCount(): Int
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collection")
    fun getAllCollection(): LiveData<List<CollectionEntry>>

    @Query("SELECT * FROM collection WHERE cardId = :cardId")
    suspend fun getEntryForCard(cardId: String): CollectionEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CollectionEntry)

    @Update
    suspend fun updateEntry(entry: CollectionEntry)

    @Delete
    suspend fun deleteEntry(entry: CollectionEntry)

    @Query("DELETE FROM collection WHERE cardId = :cardId")
    suspend fun deleteEntryByCardId(cardId: String)

    @Query("SELECT COUNT(*) FROM collection")
    fun getOwnedCardCount(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM collection")
    suspend fun getOwnedCardCountSuspend(): Int

    // Get count of owned cards per set
    @Query("""
        SELECT c.setId, COUNT(col.cardId) as ownedCount 
        FROM cards c 
        LEFT JOIN collection col ON c.id = col.cardId 
        GROUP BY c.setId
    """)
    suspend fun getOwnedCountPerSet(): List<SetOwnedCount>

    // Check if a set is complete
    @Query("""
        SELECT COUNT(*) = (SELECT COUNT(*) FROM cards WHERE setId = :setId)
        FROM collection col
        INNER JOIN cards c ON col.cardId = c.id
        WHERE c.setId = :setId
    """)
    suspend fun isSetComplete(setId: String): Boolean

    @Query("""
        SELECT COUNT(DISTINCT c.setId) FROM cards c 
        INNER JOIN collection col ON c.id = col.cardId
        WHERE (SELECT COUNT(*) FROM cards WHERE setId = c.setId) = 
              (SELECT COUNT(*) FROM collection col2 INNER JOIN cards c2 ON col2.cardId = c2.id WHERE c2.setId = c.setId)
    """)
    suspend fun getCompletedSetCount(): Int
}

data class SetOwnedCount(
    val setId: String,
    val ownedCount: Int
)
