package com.pokemontcg.tracker.ui.browse

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.data.model.PokemonSet
import com.pokemontcg.tracker.databinding.ItemBrowseHeaderBinding
import com.pokemontcg.tracker.databinding.ItemBrowseSetBinding

sealed class BrowseItem {
    data class Header(val series: String) : BrowseItem()
    data class SetItem(val set: PokemonSet) : BrowseItem()
}

class BrowseSetAdapter(
    private val onSetClick: (String) -> Unit
) : ListAdapter<BrowseItem, RecyclerView.ViewHolder>(BrowseDiffCallback()) {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SET = 1
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is BrowseItem.Header -> TYPE_HEADER
        is BrowseItem.SetItem -> TYPE_SET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(ItemBrowseHeaderBinding.inflate(inflater, parent, false))
            else -> SetViewHolder(ItemBrowseSetBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is BrowseItem.Header -> (holder as HeaderViewHolder).bind(item.series)
            is BrowseItem.SetItem -> (holder as SetViewHolder).bind(item.set)
        }
    }

    inner class HeaderViewHolder(
        private val binding: ItemBrowseHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(series: String) {
            binding.tvSeriesName.text = series
        }
    }

    inner class SetViewHolder(
        private val binding: ItemBrowseSetBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(set: PokemonSet) {
            binding.tvSetName.text = set.name
            binding.tvSetTotal.text = "${set.total} cards"
            binding.tvSetRelease.text = set.releaseDate
            binding.root.setOnClickListener { onSetClick(set.id) }
        }
    }
}

class BrowseDiffCallback : DiffUtil.ItemCallback<BrowseItem>() {
    override fun areItemsTheSame(oldItem: BrowseItem, newItem: BrowseItem): Boolean {
        return when {
            oldItem is BrowseItem.Header && newItem is BrowseItem.Header ->
                oldItem.series == newItem.series
            oldItem is BrowseItem.SetItem && newItem is BrowseItem.SetItem ->
                oldItem.set.id == newItem.set.id
            else -> false
        }
    }
    override fun areContentsTheSame(oldItem: BrowseItem, newItem: BrowseItem) = oldItem == newItem
}
