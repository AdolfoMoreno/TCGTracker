package com.pokemontcg.tracker.data.repository

import androidx.lifecycle.LiveData
import com.pokemontcg.tracker.data.db.AppDatabase
import com.pokemontcg.tracker.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PokemonRepository(private val db: AppDatabase) {

    // ── Sets ──────────────────────────────────────────────────────────────────

    fun getAllSets(): LiveData<List<PokemonSet>> = db.setDao().getAllSets()

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
}
