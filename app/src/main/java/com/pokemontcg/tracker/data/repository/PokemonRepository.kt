package com.pokemontcg.tracker.data.repository

import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.pokemontcg.tracker.data.db.AppDatabase
import com.pokemontcg.tracker.data.model.CardDetailItem
import com.pokemontcg.tracker.data.model.CardStorageSummary
import com.pokemontcg.tracker.data.model.CardWithCollection
import com.pokemontcg.tracker.data.model.CollectionEntry
import com.pokemontcg.tracker.data.model.CollectionQuantityResult
import com.pokemontcg.tracker.data.model.PokemonSet
import com.pokemontcg.tracker.data.model.SetStats
import com.pokemontcg.tracker.data.model.StorageAssignmentResult
import com.pokemontcg.tracker.data.model.StorageCardItem
import com.pokemontcg.tracker.data.model.StorageContainer
import com.pokemontcg.tracker.data.model.StorageContainerOption
import com.pokemontcg.tracker.data.model.StorageContainerSaveResult
import com.pokemontcg.tracker.data.model.StorageContainerSummary
import com.pokemontcg.tracker.data.model.StoredCardAssignment
import com.pokemontcg.tracker.data.model.Wishlist
import com.pokemontcg.tracker.data.model.WishlistCardCrossRef
import com.pokemontcg.tracker.data.model.WishlistCardItem
import com.pokemontcg.tracker.data.model.WishlistMembershipState
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import com.pokemontcg.tracker.data.model.WishlistSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    fun getCardsForSet(setId: String): LiveData<List<com.pokemontcg.tracker.data.model.PokemonCard>> =
        db.cardDao().getCardsForSet(setId)

    suspend fun getCardsWithCollectionStatus(setId: String): List<CardWithCollection> =
        withContext(Dispatchers.IO) {
            val cards = db.cardDao().getCardsForSetSuspend(setId)
            cards.map { card ->
                val entry = db.collectionDao().getEntryForCard(card.id)
                CardWithCollection(card, entry)
            }
        }

    suspend fun getCardDetail(cardId: String): CardDetailItem? = withContext(Dispatchers.IO) {
        db.cardDao().getCardDetail(cardId)
    }

    suspend fun getCardStorageSummary(cardId: String): CardStorageSummary = withContext(Dispatchers.IO) {
        val ownedQuantity = db.collectionDao().getEntryForCard(cardId)?.quantity ?: 0
        val totalStoredQuantity = db.storageDao().getTotalStoredQuantityForCard(cardId)
        CardStorageSummary(ownedQuantity = ownedQuantity, totalStoredQuantity = totalStoredQuantity)
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

    suspend fun addToCollection(cardId: String, quantity: Int = 1) = withContext(Dispatchers.IO) {
        repeat(quantity.coerceAtLeast(0)) {
            addCollectionCopyInternal(cardId, null)
        }
    }

    suspend fun removeFromCollection(cardId: String) = withContext(Dispatchers.IO) {
        db.withTransaction {
            val existing = db.collectionDao().getEntryForCard(cardId) ?: return@withTransaction
            val totalStored = db.storageDao().getTotalStoredQuantityForCard(cardId)
            when {
                totalStored <= 0 -> db.collectionDao().deleteEntryByCardId(cardId)
                existing.quantity > totalStored -> db.collectionDao()
                    .updateEntry(existing.copy(quantity = totalStored))
            }
        }
    }

    suspend fun updateCollectionEntry(entry: CollectionEntry) = withContext(Dispatchers.IO) {
        db.collectionDao().updateEntry(entry)
    }

    suspend fun isCardOwned(cardId: String): Boolean = withContext(Dispatchers.IO) {
        (db.collectionDao().getEntryForCard(cardId)?.quantity ?: 0) > 0
    }

    suspend fun getCollectionEntry(cardId: String): CollectionEntry? = withContext(Dispatchers.IO) {
        db.collectionDao().getEntryForCard(cardId)
    }

    suspend fun addCollectionCopy(cardId: String, sourceWishlistId: Long? = null): CollectionQuantityResult =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                addCollectionCopyInternal(cardId, sourceWishlistId)
                CollectionQuantityResult.Added
            }
        }

    suspend fun removeCollectionCopy(cardId: String): CollectionQuantityResult =
        withContext(Dispatchers.IO) {
            db.withTransaction {
                val existing = db.collectionDao().getEntryForCard(cardId)
                    ?: return@withTransaction CollectionQuantityResult.NoOwnedCopy
                val totalStored = db.storageDao().getTotalStoredQuantityForCard(cardId)
                if (existing.quantity - 1 < totalStored) {
                    return@withTransaction CollectionQuantityResult.BlockedByStorage
                }

                if (existing.quantity == 1) {
                    db.collectionDao().deleteEntryByCardId(cardId)
                } else {
                    db.collectionDao().updateEntry(existing.copy(quantity = existing.quantity - 1))
                }
                CollectionQuantityResult.Removed
            }
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
            db.withTransaction {
                cards.forEach { card ->
                    val existing = db.collectionDao().getEntryForCard(card.id) ?: return@forEach
                    val totalStored = db.storageDao().getTotalStoredQuantityForCard(card.id)
                    when {
                        totalStored <= 0 -> db.collectionDao().deleteEntryByCardId(card.id)
                        existing.quantity > totalStored -> db.collectionDao()
                            .updateEntry(existing.copy(quantity = totalStored))
                    }
                }
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

    suspend fun setCardWishlistMemberships(cardId: String, selectedWishlistIds: Set<Long>) =
        withContext(Dispatchers.IO) {
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
            addCollectionCopyInternal(cardId, null)
            db.wishlistDao().deleteWishlistCard(wishlistId, cardId)
            db.wishlistDao().touchWishlists(listOf(wishlistId), System.currentTimeMillis())
        }
    }

    // ── Storage ───────────────────────────────────────────────────────────────

    fun getStorageContainerSummaries(): LiveData<List<StorageContainerSummary>> =
        db.storageDao().getStorageContainerSummaries()

    suspend fun getStorageContainer(containerId: Long): StorageContainer? = withContext(Dispatchers.IO) {
        db.storageDao().getStorageContainerById(containerId)
    }

    suspend fun getStorageCards(containerId: Long): List<StorageCardItem> = withContext(Dispatchers.IO) {
        db.storageDao().getStorageCards(containerId)
    }

    suspend fun getStorageContainerOptions(cardId: String): List<StorageContainerOption> =
        withContext(Dispatchers.IO) {
            db.storageDao().getStorageContainerOptions(cardId)
        }

    suspend fun createStorageContainer(
        name: String,
        type: String,
        capacity: Int
    ): StorageContainerSaveResult = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return@withContext StorageContainerSaveResult.BlankName
        }
        if (capacity <= 0) {
            return@withContext StorageContainerSaveResult.InvalidCapacity
        }
        if (db.storageDao().getStorageNameConflictCount(trimmedName, null) > 0) {
            return@withContext StorageContainerSaveResult.DuplicateName
        }

        val now = System.currentTimeMillis()
        val containerId = db.storageDao().insertStorageContainer(
            StorageContainer(
                name = trimmedName,
                type = type,
                capacity = capacity,
                createdAt = now,
                updatedAt = now
            )
        )
        StorageContainerSaveResult.Success(containerId)
    }

    suspend fun renameStorageContainer(containerId: Long, name: String): StorageContainerSaveResult =
        withContext(Dispatchers.IO) {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@withContext StorageContainerSaveResult.BlankName
            }
            if (db.storageDao().getStorageNameConflictCount(trimmedName, containerId) > 0) {
                return@withContext StorageContainerSaveResult.DuplicateName
            }

            val existing = db.storageDao().getStorageContainerById(containerId)
                ?: return@withContext StorageContainerSaveResult.BlankName

            db.storageDao().updateStorageContainer(
                existing.copy(name = trimmedName, updatedAt = System.currentTimeMillis())
            )
            StorageContainerSaveResult.Success(containerId)
        }

    suspend fun deleteStorageContainer(containerId: Long) = withContext(Dispatchers.IO) {
        db.storageDao().getStorageContainerById(containerId)?.let {
            db.storageDao().deleteStorageContainer(it)
        }
    }

    suspend fun assignCardToStorage(
        containerId: Long,
        cardId: String,
        quantity: Int
    ): StorageAssignmentResult = withContext(Dispatchers.IO) {
        if (quantity <= 0) {
            return@withContext StorageAssignmentResult.InvalidQuantity
        }

        db.withTransaction {
            val ownedEntry = db.collectionDao().getEntryForCard(cardId)
                ?: return@withTransaction StorageAssignmentResult.NotOwned
            val container = db.storageDao().getStorageContainerById(containerId)
                ?: return@withTransaction StorageAssignmentResult.ContainerFull
            val totalStored = db.storageDao().getTotalStoredQuantityForCard(cardId)
            val availableCopies = ownedEntry.quantity - totalStored
            if (availableCopies <= 0) {
                return@withTransaction StorageAssignmentResult.NoAvailableCopies
            }

            val usedCapacity = db.storageDao().getUsedCapacity(containerId)
            val remainingCapacity = container.capacity - usedCapacity
            if (remainingCapacity <= 0) {
                return@withTransaction StorageAssignmentResult.ContainerFull
            }

            if (quantity > availableCopies) {
                return@withTransaction StorageAssignmentResult.NoAvailableCopies
            }
            if (quantity > remainingCapacity) {
                return@withTransaction StorageAssignmentResult.ContainerFull
            }

            val now = System.currentTimeMillis()
            val existing = db.storageDao().getStoredCardAssignment(containerId, cardId)
            if (existing != null) {
                db.storageDao().updateStoredCardAssignment(
                    existing.copy(quantity = existing.quantity + quantity, updatedAt = now)
                )
            } else {
                db.storageDao().insertStoredCardAssignment(
                    StoredCardAssignment(
                        containerId = containerId,
                        cardId = cardId,
                        quantity = quantity,
                        updatedAt = now
                    )
                )
            }
            db.storageDao().touchStorageContainer(containerId, now)
            StorageAssignmentResult.Success
        }
    }

    suspend fun removeCardFromStorage(
        containerId: Long,
        cardId: String,
        quantity: Int
    ): StorageAssignmentResult = withContext(Dispatchers.IO) {
        if (quantity <= 0) {
            return@withContext StorageAssignmentResult.InvalidQuantity
        }

        db.withTransaction {
            val existing = db.storageDao().getStoredCardAssignment(containerId, cardId)
                ?: return@withTransaction StorageAssignmentResult.AssignmentMissing
            if (quantity > existing.quantity) {
                return@withTransaction StorageAssignmentResult.InvalidQuantity
            }

            val now = System.currentTimeMillis()
            if (quantity == existing.quantity) {
                db.storageDao().deleteStoredCardAssignment(existing)
            } else {
                db.storageDao().updateStoredCardAssignment(
                    existing.copy(quantity = existing.quantity - quantity, updatedAt = now)
                )
            }
            db.storageDao().touchStorageContainer(containerId, now)
            StorageAssignmentResult.Success
        }
    }

    suspend fun toggleCollectionFromDetail(cardId: String) = withContext(Dispatchers.IO) {
        val existing = db.collectionDao().getEntryForCard(cardId)
        if ((existing?.quantity ?: 0) > 0) {
            removeCollectionCopy(cardId)
        } else {
            addCollectionCopy(cardId)
        }
    }

    private suspend fun addCollectionCopyInternal(cardId: String, sourceWishlistId: Long?) {
        val existing = db.collectionDao().getEntryForCard(cardId)
        if (existing != null) {
            db.collectionDao().updateEntry(existing.copy(quantity = existing.quantity + 1))
        } else {
            db.collectionDao().insertEntry(CollectionEntry(cardId = cardId, quantity = 1))
        }

        if (sourceWishlistId != null) {
            db.wishlistDao().deleteWishlistCard(sourceWishlistId, cardId)
            db.wishlistDao().touchWishlists(listOf(sourceWishlistId), System.currentTimeMillis())
        }
    }
}
