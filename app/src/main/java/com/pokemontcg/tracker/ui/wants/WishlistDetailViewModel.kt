package com.pokemontcg.tracker.ui.wants

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pokemontcg.tracker.data.model.CardStorageSummary
import com.pokemontcg.tracker.data.model.CollectionQuantityResult
import com.pokemontcg.tracker.data.model.StorageAssignmentResult
import com.pokemontcg.tracker.data.model.StorageContainerOption
import com.pokemontcg.tracker.data.model.Wishlist
import com.pokemontcg.tracker.data.model.WishlistCardItem
import com.pokemontcg.tracker.data.model.WishlistMembershipState
import com.pokemontcg.tracker.data.model.WishlistSaveResult
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

    suspend fun getWishlistMembershipStates(cardId: String): List<WishlistMembershipState> {
        return repository.getWishlistMembershipStates(cardId)
    }

    suspend fun setCardWishlistMemberships(cardId: String, selectedWishlistIds: Set<Long>) {
        repository.setCardWishlistMemberships(cardId, selectedWishlistIds)
        refresh()
    }

    suspend fun createWishlist(name: String): WishlistSaveResult {
        return repository.createWishlist(name)
    }

    suspend fun getStorageContainerOptions(cardId: String): List<StorageContainerOption> {
        return repository.getStorageContainerOptions(cardId)
    }

    suspend fun getCardStorageSummary(cardId: String): CardStorageSummary {
        return repository.getCardStorageSummary(cardId)
    }

    suspend fun assignCardToStorage(cardId: String, quantity: Int, containerId: Long): StorageAssignmentResult {
        val result = repository.assignCardToStorage(containerId, cardId, quantity)
        refresh()
        return result
    }

    suspend fun addCopy(cardId: String): CollectionQuantityResult {
        val result = if ((repository.getCollectionEntry(cardId)?.quantity ?: 0) > 0) {
            repository.addCollectionCopy(cardId)
        } else {
            repository.addCollectionCopy(cardId, wishlistId)
        }
        refresh()
        return result
    }

    suspend fun removeCopy(cardId: String): CollectionQuantityResult {
        val result = repository.removeCollectionCopy(cardId)
        refresh()
        return result
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
