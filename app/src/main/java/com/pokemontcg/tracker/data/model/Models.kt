package com.pokemontcg.tracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

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
