package com.pokemontcg.tracker.ui.wants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pokemontcg.tracker.data.model.Wishlist
import com.pokemontcg.tracker.data.model.WishlistCardItem
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class WishlistDetailViewModel(
    private val repository: PokemonRepository,
    private val wishlistId: Long
) : ViewModel() {

    private val _wishlist = MutableLiveData<Wishlist?>()
    val wishlist: LiveData<Wishlist?> = _wishlist

    private val _cards = MutableLiveData<List<WishlistCardItem>>(emptyList())
    val cards: LiveData<List<WishlistCardItem>> = _cards

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _wishlist.value = repository.getWishlist(wishlistId)
                _cards.value = repository.getWishlistCards(wishlistId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markCardAsCollected(cardId: String) {
        viewModelScope.launch {
            repository.markWishlistCardAsCollected(wishlistId, cardId)
            refresh()
        }
    }

    fun removeCard(cardId: String) {
        viewModelScope.launch {
            repository.removeCardFromWishlist(wishlistId, cardId)
            refresh()
        }
    }
}

class WishlistDetailViewModelFactory(
    private val repository: PokemonRepository,
    private val wishlistId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WishlistDetailViewModel(repository, wishlistId) as T
    }
}
