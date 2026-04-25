package com.pokemontcg.tracker.ui.browse

import androidx.lifecycle.*
import com.pokemontcg.tracker.data.model.PokemonSet
import com.pokemontcg.tracker.data.repository.PokemonRepository
import kotlinx.coroutines.launch

class BrowseViewModel(private val repository: PokemonRepository) : ViewModel() {

    private val seriesDisplayOrder = listOf(
        "Mega Evolution",
        "Scarlet & Violet",
        "Sword & Shield",
        "Sun & Moon",
        "XY",
        "Black & White",
        "HeartGold & SoulSilver",
        "Platinum",
        "Diamond & Pearl",
        "EX",
        "POP",
        "NP",
        "E-Card",
        "Neo",
        "Gym",
        "Base",
        "Other",
    )

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
            .toList()
            .sortedWith(
                compareBy<Pair<String, List<PokemonSet>>> { seriesSortOrder(it.first) }
                    .thenBy { it.first.lowercase() }
            )
            .associateTo(linkedMapOf()) { (series, groupedSets) ->
                series to groupedSets
            }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun seriesSortOrder(series: String): Int {
        val index = seriesDisplayOrder.indexOf(series)
        return if (index >= 0) index else Int.MAX_VALUE
    }
}

class BrowseViewModelFactory(private val repository: PokemonRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return BrowseViewModel(repository) as T
    }
}
