package com.pokemontcg.tracker.ui.home

import androidx.lifecycle.*
import com.pokemontcg.tracker.data.model.CardWithCollection
import com.pokemontcg.tracker.data.model.CardDetailItem
import com.pokemontcg.tracker.data.model.SetStats
import com.pokemontcg.tracker.data.model.WishlistMembershipState
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class SetDetailViewModel(
    private val repository: PokemonRepository,
    private val setId: String
) : ViewModel() {

    private val _cards = MutableLiveData<List<CardWithCollection>>()
    val cards: LiveData<List<CardWithCollection>> = _cards

    private val _setStats = MutableLiveData<SetStats?>()
    val setStats: LiveData<SetStats?> = _setStats

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _filterOwned = MutableLiveData(false)
    val filterOwned: LiveData<Boolean> = _filterOwned

    val filteredCards: LiveData<List<CardWithCollection>> = MediatorLiveData<List<CardWithCollection>>().apply {
        fun update() {
            val all = _cards.value ?: return
            val onlyOwned = _filterOwned.value ?: false
            value = if (onlyOwned) all.filter { it.isOwned } else all
        }
        addSource(_cards) { update() }
        addSource(_filterOwned) { update() }
    }

    init {
        loadSetData()
    }

    fun loadSetData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cards = repository.getCardsWithCollectionStatus(setId)
                _cards.value = cards
                val allStats = repository.getAllSetStats()
                _setStats.value = allStats.find { it.set.id == setId }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCard(cardId: String) {
        viewModelScope.launch {
            val isOwned = repository.isCardOwned(cardId)
            if (isOwned) {
                repository.removeFromCollection(cardId)
            } else {
                repository.addToCollection(cardId)
            }
            loadSetData()
        }
    }

    fun setFilterOwned(filter: Boolean) {
        _filterOwned.value = filter
    }

    fun selectAllCards(owned: Boolean) {
        viewModelScope.launch {
            repository.toggleSetCollection(setId, owned)
            loadSetData()
        }
    }

    suspend fun getWishlistMembershipStates(cardId: String): List<WishlistMembershipState> {
        return repository.getWishlistMembershipStates(cardId)
    }

    suspend fun setCardWishlistMemberships(cardId: String, selectedWishlistIds: Set<Long>) {
        repository.setCardWishlistMemberships(cardId, selectedWishlistIds)
    }

    suspend fun createWishlist(name: String): WishlistSaveResult {
        return repository.createWishlist(name)
    }

    suspend fun getCardDetail(cardId: String): CardDetailItem? {
        return repository.getCardDetail(cardId)
    }

    suspend fun toggleCollectionFromDetail(cardId: String) {
        repository.toggleCollectionFromDetail(cardId)
        loadSetData()
    }
}

class SetDetailViewModelFactory(
    private val repository: PokemonRepository,
    private val setId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return SetDetailViewModel(repository, setId) as T
    }
}
