package com.pokemontcg.tracker.ui.card

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pokemontcg.tracker.data.model.CardDetailItem
import com.pokemontcg.tracker.data.model.WishlistMembershipState
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val repository: PokemonRepository,
    private val cardId: String,
    private val sourceWishlistId: Long?
) : ViewModel() {

    private val _card = MutableLiveData<CardDetailItem?>()
    val card: LiveData<CardDetailItem?> = _card

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _card.value = repository.getCardDetail(cardId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun performCollectionAction() {
        viewModelScope.launch {
            val current = _card.value ?: return@launch
            if (!current.isOwned && sourceWishlistId != null) {
                repository.markWishlistCardAsCollected(sourceWishlistId, cardId)
            } else {
                repository.toggleCollectionFromDetail(cardId)
            }
            refresh()
        }
    }

    suspend fun getWishlistMembershipStates(): List<WishlistMembershipState> {
        return repository.getWishlistMembershipStates(cardId)
    }

    suspend fun setCardWishlistMemberships(selectedWishlistIds: Set<Long>) {
        repository.setCardWishlistMemberships(cardId, selectedWishlistIds)
        refresh()
    }

    suspend fun createWishlist(name: String): WishlistSaveResult {
        return repository.createWishlist(name)
    }
}

class CardDetailViewModelFactory(
    private val repository: PokemonRepository,
    private val cardId: String,
    private val sourceWishlistId: Long?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CardDetailViewModel(repository, cardId, sourceWishlistId) as T
    }
}
