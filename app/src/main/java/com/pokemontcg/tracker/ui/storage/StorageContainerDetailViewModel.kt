package com.pokemontcg.tracker.ui.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pokemontcg.tracker.data.model.CardStorageSummary
import com.pokemontcg.tracker.data.model.StorageAssignmentResult
import com.pokemontcg.tracker.data.model.StorageCardItem
import com.pokemontcg.tracker.data.model.StorageContainer
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class StorageContainerDetailViewModel(
    private val repository: PokemonRepository,
    private val containerId: Long
) : ViewModel() {

    private val _container = MutableLiveData<StorageContainer?>()
    val container: LiveData<StorageContainer?> = _container

    private val _cards = MutableLiveData<List<StorageCardItem>>(emptyList())
    val cards: LiveData<List<StorageCardItem>> = _cards

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _container.value = repository.getStorageContainer(containerId)
                _cards.value = repository.getStorageCards(containerId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun addCopies(cardId: String, quantity: Int): StorageAssignmentResult {
        val result = repository.assignCardToStorage(containerId, cardId, quantity)
        refresh()
        return result
    }

    suspend fun removeCopies(cardId: String, quantity: Int): StorageAssignmentResult {
        val result = repository.removeCardFromStorage(containerId, cardId, quantity)
        refresh()
        return result
    }

    suspend fun getCardStorageSummary(cardId: String): CardStorageSummary {
        return repository.getCardStorageSummary(cardId)
    }
}

class StorageContainerDetailViewModelFactory(
    private val repository: PokemonRepository,
    private val containerId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StorageContainerDetailViewModel(repository, containerId) as T
    }
}
