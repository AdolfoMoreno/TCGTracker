package com.pokemontcg.tracker.data.repository

import androidx.lifecycle.LiveData
import com.pokemontcg.tracker.data.db.AppDatabase
import com.pokemontcg.tracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.room.withTransaction

class PokemonRepository(private val db: AppDatabase) {

    // ── Sets ──────────────────────────────────────────────────────────────────

    fun getAllSets(): LiveData<List<PokemonSet>> = db.setDao().getAllSets()

    suspend fun getAllSetCount(): Int = withContext(Dispatchers.IO) {
        db.setDao().getSetCount()
    }

    suspend fun getAllSetStats(): List<SetStats> = withContext(Dispatchers.IO) {
        val sets = db.setDao().getAllSetsSuspend()
        val ownedCounts = db.collectionDao().getOwnedCountPerSet()
            .associateBy { it.setId }

        sets.map { set ->
            val totalCards = db.cardDao().getCardCountForSet(set.id)
            val ownedCount = ownedCounts[set.id]?.ownedCount ?: 0
            SetStats(set, ownedCount, totalCards)
        }
    }

    // ── Cards ─────────────────────────────────────────────────────────────────

    fun getCardsForSet(setId: String): LiveData<List<PokemonCard>> =
        db.cardDao().getCardsForSet(setId)

    suspend fun getCardsWithCollectionStatus(setId: String): List<CardWithCollection> =
        withContext(Dispatchers.IO) {
            val cards = db.cardDao().getCardsForSetSuspend(setId)
            cards.map { card ->
                val entry = db.collectionDao().getEntryForCard(card.id)
                CardWithCollection(card, entry)
            }
        }

    // ── Collection ────────────────────────────────────────────────────────────

    fun getOwnedCardCount(): LiveData<Int> = db.collectionDao().getOwnedCardCount()

    suspend fun getOwnedCardCountSuspend(): Int = withContext(Dispatchers.IO) {
        db.collectionDao().getOwnedCardCountSuspend()
    }

    suspend fun getTotalCardCount(): Int = withContext(Dispatchers.IO) {
        db.cardDao().getTotalCardCount()
    }

    suspend fun getCompletedSetCount(): Int = withContext(Dispatchers.IO) {
        db.collectionDao().getCompletedSetCount()
    }

    suspend fun addToCollection(cardId: String, quantity: Int = 1) =
        withContext(Dispatchers.IO) {
            val existing = db.collectionDao().getEntryForCard(cardId)
            if (existing != null) {
                db.collectionDao().updateEntry(existing.copy(quantity = existing.quantity + quantity))
            } else {
                db.collectionDao().insertEntry(CollectionEntry(cardId = cardId, quantity = quantity))
            }
        }

    suspend fun removeFromCollection(cardId: String) = withContext(Dispatchers.IO) {
        db.collectionDao().deleteEntryByCardId(cardId)
    }

    suspend fun updateCollectionEntry(entry: CollectionEntry) = withContext(Dispatchers.IO) {
        db.collectionDao().updateEntry(entry)
    }

    suspend fun isCardOwned(cardId: String): Boolean = withContext(Dispatchers.IO) {
        db.collectionDao().getEntryForCard(cardId) != null
    }

    suspend fun getCollectionEntry(cardId: String): CollectionEntry? =
        withContext(Dispatchers.IO) {
            db.collectionDao().getEntryForCard(cardId)
        }

    suspend fun toggleSetCollection(setId: String, owned: Boolean) = withContext(Dispatchers.IO) {
        val cards = db.cardDao().getCardsForSetSuspend(setId)
        if (owned) {
            cards.forEach { card ->
                if (db.collectionDao().getEntryForCard(card.id) == null) {
                    db.collectionDao().insertEntry(CollectionEntry(cardId = card.id))
                }
            }
        } else {
            cards.forEach { card ->
                db.collectionDao().deleteEntryByCardId(card.id)
            }
        }
    }

    // ── Wishlists ─────────────────────────────────────────────────────────────

    fun getWishlistSummaries(): LiveData<List<WishlistSummary>> = db.wishlistDao().getWishlistSummaries()

    suspend fun getWishlist(wishlistId: Long): Wishlist? = withContext(Dispatchers.IO) {
        db.wishlistDao().getWishlistById(wishlistId)
    }

    suspend fun getWishlistCards(wishlistId: Long): List<WishlistCardItem> = withContext(Dispatchers.IO) {
        db.wishlistDao().getWishlistCards(wishlistId)
    }

    suspend fun getWishlistMembershipStates(cardId: String): List<WishlistMembershipState> =
        withContext(Dispatchers.IO) {
            db.wishlistDao().getWishlistMembershipStates(cardId)
        }

    suspend fun createWishlist(name: String): WishlistSaveResult = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return@withContext WishlistSaveResult.BlankName
        }
        if (db.wishlistDao().getWishlistNameConflictCount(trimmedName, null) > 0) {
            return@withContext WishlistSaveResult.DuplicateName
        }

        val now = System.currentTimeMillis()
        val wishlistId = db.wishlistDao().insertWishlist(
            Wishlist(name = trimmedName, createdAt = now, updatedAt = now)
        )
        WishlistSaveResult.Success(wishlistId)
    }

    suspend fun renameWishlist(wishlistId: Long, name: String): WishlistSaveResult =
        withContext(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@withContext WishlistSaveResult.BlankName
            }
            if (db.wishlistDao().getWishlistNameConflictCount(trimmedName, wishlistId) > 0) {
                return@withContext WishlistSaveResult.DuplicateName
            }

            val existing = db.wishlistDao().getWishlistById(wishlistId)
                ?: return@withContext WishlistSaveResult.BlankName

            val now = System.currentTimeMillis()
            db.wishlistDao().updateWishlist(existing.copy(name = trimmedName, updatedAt = now))
            WishlistSaveResult.Success(wishlistId)
        }

    suspend fun deleteWishlist(wishlistId: Long) = withContext(Dispatchers.IO) {
        db.wishlistDao().getWishlistById(wishlistId)?.let { db.wishlistDao().deleteWishlist(it) }
    }

    suspend fun setCardWishlistMemberships(
        cardId: String,
        selectedWishlistIds: Set<Long>
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val now = System.currentTimeMillis()
            val currentIds = db.wishlistDao().getWishlistIdsForCard(cardId).toSet()
            val targetIds = selectedWishlistIds

            val idsToAdd = targetIds - currentIds
            val idsToRemove = currentIds - targetIds

            idsToAdd.forEach { wishlistId ->
                db.wishlistDao().insertWishlistCard(
                    WishlistCardCrossRef(wishlistId = wishlistId, cardId = cardId, addedAt = now)
                )
            }
            idsToRemove.forEach { wishlistId ->
                db.wishlistDao().deleteWishlistCard(wishlistId, cardId)
            }

            val touchedIds = (idsToAdd + idsToRemove).toList()
            if (touchedIds.isNotEmpty()) {
                db.wishlistDao().touchWishlists(touchedIds, now)
            }
        }
    }

    suspend fun removeCardFromWishlist(wishlistId: Long, cardId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            db.wishlistDao().deleteWishlistCard(wishlistId, cardId)
            db.wishlistDao().touchWishlists(listOf(wishlistId), System.currentTimeMillis())
        }
    }

    suspend fun markWishlistCardAsCollected(wishlistId: Long, cardId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = db.collectionDao().getEntryForCard(cardId)
            if (existing != null) {
                db.collectionDao().updateEntry(existing.copy(quantity = existing.quantity + 1))
            } else {
                db.collectionDao().insertEntry(CollectionEntry(cardId = cardId, quantity = 1))
            }
            db.wishlistDao().deleteWishlistCard(wishlistId, cardId)
            db.wishlistDao().touchWishlists(listOf(wishlistId), System.currentTimeMillis())
        }
    }
}
