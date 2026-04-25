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

    @Query("""
        SELECT c.id, c.name, c.number, c.setId, c.rarity, c.types, c.supertype,
               c.imageSmall, c.imageLarge,
               s.name AS setName, s.series AS setSeries, s.releaseDate AS releaseDate,
               COALESCE(col.quantity, 0) AS ownedQuantity
        FROM cards c
        INNER JOIN sets s ON s.id = c.setId
        LEFT JOIN collection col ON col.cardId = c.id
        WHERE c.id = :cardId
    """)
    suspend fun getCardDetail(cardId: String): CardDetailItem?

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

@Dao
interface WishlistDao {
    @Query("""
        SELECT w.id, w.name,
               COUNT(wc.cardId) AS cardCount,
               COALESCE(SUM(CASE WHEN col.cardId IS NOT NULL THEN 1 ELSE 0 END), 0) AS ownedCount
        FROM wishlists w
        LEFT JOIN wishlist_cards wc ON wc.wishlistId = w.id
        LEFT JOIN collection col ON col.cardId = wc.cardId
        GROUP BY w.id
        ORDER BY w.updatedAt DESC, w.name COLLATE NOCASE ASC
    """)
    fun getWishlistSummaries(): LiveData<List<WishlistSummary>>

    @Query("SELECT * FROM wishlists ORDER BY updatedAt DESC, name COLLATE NOCASE ASC")
    suspend fun getAllWishlists(): List<Wishlist>

    @Query("SELECT * FROM wishlists WHERE id = :wishlistId")
    suspend fun getWishlistById(wishlistId: Long): Wishlist?

    @Query("""
        SELECT COUNT(*) FROM wishlists
        WHERE LOWER(name) = LOWER(:name) AND id != COALESCE(:excludeId, -1)
    """)
    suspend fun getWishlistNameConflictCount(name: String, excludeId: Long?): Int

    @Insert
    suspend fun insertWishlist(wishlist: Wishlist): Long

    @Update
    suspend fun updateWishlist(wishlist: Wishlist)

    @Delete
    suspend fun deleteWishlist(wishlist: Wishlist)

    @Query("UPDATE wishlists SET updatedAt = :updatedAt WHERE id IN (:wishlistIds)")
    suspend fun touchWishlists(wishlistIds: List<Long>, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWishlistCard(crossRef: WishlistCardCrossRef): Long

    @Query("DELETE FROM wishlist_cards WHERE wishlistId = :wishlistId AND cardId = :cardId")
    suspend fun deleteWishlistCard(wishlistId: Long, cardId: String)

    @Query("SELECT wishlistId FROM wishlist_cards WHERE cardId = :cardId")
    suspend fun getWishlistIdsForCard(cardId: String): List<Long>

    @Query("""
        SELECT w.id, w.name,
               EXISTS(
                   SELECT 1 FROM wishlist_cards wc
                   WHERE wc.wishlistId = w.id AND wc.cardId = :cardId
               ) AS isSelected
        FROM wishlists w
        ORDER BY w.updatedAt DESC, w.name COLLATE NOCASE ASC
    """)
    suspend fun getWishlistMembershipStates(cardId: String): List<WishlistMembershipState>

    @Query("""
        SELECT c.id, c.name, c.number, c.setId, c.rarity, c.types, c.supertype,
               c.imageSmall, c.imageLarge,
               s.name AS setName, s.series AS setSeries, s.releaseDate AS releaseDate,
               COALESCE(col.quantity, 0) AS ownedQuantity
        FROM wishlist_cards wc
        INNER JOIN cards c ON c.id = wc.cardId
        INNER JOIN sets s ON s.id = c.setId
        LEFT JOIN collection col ON col.cardId = c.id
        WHERE wc.wishlistId = :wishlistId
        ORDER BY s.releaseDate DESC,
                 CASE WHEN c.number GLOB '[0-9]*' THEN CAST(c.number AS INTEGER) ELSE 999999 END,
                 c.number ASC
    """)
    suspend fun getWishlistCards(wishlistId: Long): List<WishlistCardItem>
}

data class SetOwnedCount(
    val setId: String,
    val ownedCount: Int
)
