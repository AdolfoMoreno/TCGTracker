package com.pokemontcg.tracker.ui.browse

import androidx.lifecycle.*
import com.pokemontcg.tracker.data.model.PokemonSet
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class BrowseViewModel(private val repository: PokemonRepository) : ViewModel() {

    val allSets: LiveData<List<PokemonSet>> = repository.getAllSets()

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    val filteredSets: LiveData<List<PokemonSet>> = MediatorLiveData<List<PokemonSet>>().apply {
        fun update() {
            val sets = allSets.value ?: emptyList()
            val query = _searchQuery.value ?: ""
            value = if (query.isBlank()) sets
            else sets.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.series.contains(query, ignoreCase = true)
            }
        }
        addSource(allSets) { update() }
        addSource(_searchQuery) { update() }
    }

    // Group by series
    val setsBySeries: LiveData<Map<String, List<PokemonSet>>> = filteredSets.map { sets ->
        sets.groupBy { it.series }
            .toSortedMap(compareByDescending { seriesSortOrder(it) })
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun seriesSortOrder(series: String): Int = when {
        series.contains("Scarlet & Violet") -> 6
        series.contains("Sword & Shield") -> 5
        series.contains("Sun & Moon") -> 4
        series.contains("XY") -> 3
        series.contains("Black & White") -> 2
        series.contains("Diamond") || series.contains("HeartGold") -> 1
        else -> 0
    }
}

class BrowseViewModelFactory(private val repository: PokemonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BrowseViewModel(repository) as T
    }
}
