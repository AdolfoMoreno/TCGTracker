package com.pokemontcg.tracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pokemontcg.tracker.data.model.CollectionEntry
import com.pokemontcg.tracker.data.model.PokemonCard
import com.pokemontcg.tracker.data.model.PokemonSet
import com.pokemontcg.tracker.data.model.Wishlist
import com.pokemontcg.tracker.data.model.WishlistCardCrossRef

@Database(
    entities = [
        PokemonSet::class,
        PokemonCard::class,
        CollectionEntry::class,
        Wishlist::class,
        WishlistCardCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun setDao(): SetDao
    abstract fun cardDao(): CardDao
    abstract fun collectionDao(): CollectionDao
    abstract fun wishlistDao(): WishlistDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wishlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wishlist_cards` (
                        `wishlistId` INTEGER NOT NULL,
                        `cardId` TEXT NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`wishlistId`, `cardId`),
                        FOREIGN KEY(`wishlistId`) REFERENCES `wishlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_wishlist_cards_cardId` ON `wishlist_cards` (`cardId`)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pokemon_tcg_tracker.db"
                )
                    // Fresh installs copy the bundled asset once, then continue using the
                    // app-local Room database. Future schema changes should use explicit
                    // migrations to preserve collection data.
                    .createFromAsset("database/pokemon_tcg_tracker.db")
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
