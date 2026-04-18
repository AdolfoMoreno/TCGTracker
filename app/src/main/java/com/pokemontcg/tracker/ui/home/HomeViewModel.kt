package com.pokemontcg.tracker.ui.home

import androidx.lifecycle.*
import com.pokemontcg.tracker.data.model.SetStats
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

data class HomeStats(
    val ownedCards: Int = 0,
    val totalCards: Int = 0,
    val completedSets: Int = 0,
    val totalSets: Int = 0
)

class HomeViewModel(private val repository: PokemonRepository) : ViewModel() {

    private val _stats = MutableLiveData<HomeStats>()
    val stats: LiveData<HomeStats> = _stats

    private val _setStats = MutableLiveData<List<SetStats>>()
    val setStats: LiveData<List<SetStats>> = _setStats

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadData()
        // Observe owned count changes to refresh stats
        repository.getOwnedCardCount().observeForever { refreshStats() }
    }

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshStats()
                val sets = repository.getAllSetStats()
                _setStats.value = sets
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun refreshStats() {
        viewModelScope.launch {
            val ownedCards = repository.getOwnedCardCount().value ?: 0
            val totalCards = repository.getTotalCardCount()
            val completedSets = repository.getCompletedSetCount()
            val totalSets = (_setStats.value?.size ?: 0)
            _stats.value = HomeStats(ownedCards, totalCards, completedSets, totalSets)
        }
    }

    fun refreshCollection() = loadData()
}

class HomeViewModelFactory(private val repository: PokemonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return HomeViewModel(repository) as T
    }
}
