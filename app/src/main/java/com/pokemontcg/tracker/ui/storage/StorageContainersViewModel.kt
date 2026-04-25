package com.pokemontcg.tracker.ui.storage

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pokemontcg.tracker.data.model.StorageContainerSaveResult
import com.pokemontcg.tracker.data.model.StorageContainerSummary
import com.pokemontcg.tracker.data.repository.PokemonRepository

class StorageContainersViewModel(private val repository: PokemonRepository) : ViewModel() {

    val containers: LiveData<List<StorageContainerSummary>> = repository.getStorageContainerSummaries()

    suspend fun createContainer(
        name: String,
        type: String,
        capacity: Int
    ): StorageContainerSaveResult = repository.createStorageContainer(name, type, capacity)

    suspend fun renameContainer(containerId: Long, name: String): StorageContainerSaveResult =
        repository.renameStorageContainer(containerId, name)

    suspend fun deleteContainer(containerId: Long) {
        repository.deleteStorageContainer(containerId)
    }
}

class StorageContainersViewModelFactory(private val repository: PokemonRepository) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StorageContainersViewModel(repository) as T
    }
}
