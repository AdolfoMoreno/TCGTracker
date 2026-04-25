package com.pokemontcg.tracker.ui.wants

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import com.pokemontcg.tracker.data.model.WishlistSummary
import com.pokemontcg.tracker.data.repository.PokemonRepository

class WishlistsViewModel(private val repository: PokemonRepository) : ViewModel() {

    val wishlists: LiveData<List<WishlistSummary>> = repository.getWishlistSummaries()

    suspend fun createWishlist(name: String): WishlistSaveResult {
        return repository.createWishlist(name)
    }

    suspend fun renameWishlist(wishlistId: Long, name: String): WishlistSaveResult {
        return repository.renameWishlist(wishlistId, name)
    }

    suspend fun deleteWishlist(wishlistId: Long) {
        repository.deleteWishlist(wishlistId)
    }
}

class WishlistsViewModelFactory(private val repository: PokemonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WishlistsViewModel(repository) as T
    }
}
