package com.pokemontcg.tracker

import android.app.Application
import com.pokemontcg.tracker.data.db.AppDatabase
import com.pokemontcg.tracker.data.repository.PokemonRepository

class PokemonApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val repository by lazy { PokemonRepository(database) }
}
