package com.pokemontcg.tracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a Pokemon TCG Set (e.g., "Scarlet & Violet Base Set")
 */
@Entity(tableName = "sets")
data class PokemonSet(
    @PrimaryKey val id: String,          // e.g. "sv1"
    val name: String,                     // e.g. "Scarlet & Violet"
    val series: String,                   // e.g. "Scarlet & Violet"
    val printedTotal: Int,                // Cards printed in the set
    val total: Int,                       // Total cards including secret rares
    val releaseDate: String,              // "2023/03/31"
    val logoUrl: String = "",             // URL to set logo (for future online use)
    val symbolUrl: String = ""            // URL to set symbol
)

/**
 * Represents a single Pokemon card
 */
@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = PokemonSet::class,
        parentColumns = ["id"],
        childColumns = ["setId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("setId")]
)
data class PokemonCard(
    @PrimaryKey val id: String,           // e.g. "sv1-1"
    val name: String,                     // e.g. "Sprigatito"
    val number: String,                   // e.g. "1" or "TG01"
    val setId: String,                    // FK to PokemonSet
    val rarity: String,                   // e.g. "Common", "Rare Holo"
    val types: String,                    // Comma-separated, e.g. "Grass"
    val supertype: String,                // "Pokémon", "Trainer", "Energy"
    val imageSmall: String = "",          // local asset path or URL
    val imageLarge: String = ""
)

/**
 * Tracks which cards the user owns
 */
@Entity(
    tableName = "collection",
    foreignKeys = [ForeignKey(
        entity = PokemonCard::class,
        parentColumns = ["id"],
        childColumns = ["cardId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("cardId")]
)
data class CollectionEntry(
    @PrimaryKey val cardId: String,
    val quantity: Int = 1,
    val condition: String = "Near Mint",  // NM, LP, MP, HP, D
    val isFoil: Boolean = false,
    val notes: String = "",
    val dateAdded: Long = System.currentTimeMillis()
)

object StorageContainerType {
    const val BINDER = "binder"
    const val BOX = "box"
}

/**
 * Named storage container for owned copies.
 */
@Entity(tableName = "storage_containers")
data class StorageContainer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val capacity: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Per-copy assignment of a card into a storage container.
 */
@Entity(
    tableName = "stored_card_assignments",
    primaryKeys = ["containerId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = StorageContainer::class,
            parentColumns = ["id"],
            childColumns = ["containerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PokemonCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class StoredCardAssignment(
    val containerId: Long,
    val cardId: String,
    val quantity: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * User-created wishlist grouping multiple wanted cards.
 */
@Entity(tableName = "wishlists")
data class Wishlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Join table mapping cards to one or more wishlists.
 */
@Entity(
    tableName = "wishlist_cards",
    primaryKeys = ["wishlistId", "cardId"],
    foreignKeys = [
        ForeignKey(
            entity = Wishlist::class,
            parentColumns = ["id"],
            childColumns = ["wishlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PokemonCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class WishlistCardCrossRef(
    val wishlistId: Long,
    val cardId: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * Combined view model for a card with its collection status
 */
data class CardWithCollection(
    val card: PokemonCard,
    val entry: CollectionEntry?           // null = not owned
) {
    val isOwned: Boolean get() = entry != null
    val quantity: Int get() = entry?.quantity ?: 0
}

/**
 * Summary stats for a set
 */
data class SetStats(
    val set: PokemonSet,
    val ownedCount: Int,
    val totalCount: Int
) {
    val completionPercent: Float get() =
        if (totalCount == 0) 0f else (ownedCount.toFloat() / totalCount) * 100f
    val isComplete: Boolean get() = ownedCount >= totalCount && totalCount > 0
}

data class WishlistSummary(
    val id: Long,
    val name: String,
    val cardCount: Int,
    val ownedCount: Int
)

data class WishlistMembershipState(
    val id: Long,
    val name: String,
    val isSelected: Boolean
)

data class WishlistCardItem(
    val id: String,
    val name: String,
    val number: String,
    val setId: String,
    val rarity: String,
    val types: String,
    val supertype: String,
    val imageSmall: String,
    val imageLarge: String,
    val setName: String,
    val setSeries: String,
    val releaseDate: String,
    val ownedQuantity: Int
) {
    val isOwned: Boolean get() = ownedQuantity > 0
}

data class CardDetailItem(
    val id: String,
    val name: String,
    val number: String,
    val setId: String,
    val rarity: String,
    val types: String,
    val supertype: String,
    val imageSmall: String,
    val imageLarge: String,
    val setName: String,
    val setSeries: String,
    val releaseDate: String,
    val ownedQuantity: Int,
    val totalStoredQuantity: Int
) {
    val isOwned: Boolean get() = ownedQuantity > 0
    val availableToAssign: Int get() = (ownedQuantity - totalStoredQuantity).coerceAtLeast(0)
}

data class StorageContainerSummary(
    val id: Long,
    val name: String,
    val type: String,
    val capacity: Int,
    val usedCapacity: Int,
    val storedCardCount: Int
) {
    val remainingCapacity: Int get() = (capacity - usedCapacity).coerceAtLeast(0)
}

data class StorageCardItem(
    val id: String,
    val name: String,
    val number: String,
    val setId: String,
    val rarity: String,
    val types: String,
    val supertype: String,
    val imageSmall: String,
    val imageLarge: String,
    val setName: String,
    val setSeries: String,
    val releaseDate: String,
    val ownedQuantity: Int,
    val storedQuantity: Int
) {
    val isOwned: Boolean get() = ownedQuantity > 0
}

data class StorageContainerOption(
    val id: Long,
    val name: String,
    val type: String,
    val capacity: Int,
    val usedCapacity: Int,
    val storedHereQuantity: Int
) {
    val remainingCapacity: Int get() = (capacity - usedCapacity).coerceAtLeast(0)
}

data class CardStorageSummary(
    val ownedQuantity: Int,
    val totalStoredQuantity: Int
) {
    val availableToAssign: Int get() = (ownedQuantity - totalStoredQuantity).coerceAtLeast(0)
}

sealed class WishlistSaveResult {
    data class Success(val wishlistId: Long) : WishlistSaveResult()
    data object BlankName : WishlistSaveResult()
    data object DuplicateName : WishlistSaveResult()
}

sealed class StorageContainerSaveResult {
    data class Success(val containerId: Long) : StorageContainerSaveResult()
    data object BlankName : StorageContainerSaveResult()
    data object DuplicateName : StorageContainerSaveResult()
    data object InvalidCapacity : StorageContainerSaveResult()
}

sealed class StorageAssignmentResult {
    data object Success : StorageAssignmentResult()
    data object InvalidQuantity : StorageAssignmentResult()
    data object NotOwned : StorageAssignmentResult()
    data object NoAvailableCopies : StorageAssignmentResult()
    data object ContainerFull : StorageAssignmentResult()
    data object AssignmentMissing : StorageAssignmentResult()
}

sealed class CollectionQuantityResult {
    data object Added : CollectionQuantityResult()
    data object Removed : CollectionQuantityResult()
    data object NoOwnedCopy : CollectionQuantityResult()
    data object BlockedByStorage : CollectionQuantityResult()
}
