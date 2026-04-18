package com.pokemontcg.tracker.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.data.model.SetStats
import com.pokemontcg.tracker.databinding.ItemSetCollectionBinding

class SetCollectionAdapter(
    private val onSetClick: (String) -> Unit
) : ListAdapter<SetStats, SetCollectionAdapter.SetViewHolder>(SetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val binding = ItemSetCollectionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SetViewHolder(
        private val binding: ItemSetCollectionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stats: SetStats) {
            binding.tvSetName.text = stats.set.name
            binding.tvSetSeries.text = stats.set.series
            binding.tvProgress.text = "${stats.ownedCount} / ${stats.totalCount}"
            binding.progressBar.max = stats.totalCount
            binding.progressBar.progress = stats.ownedCount

            if (stats.isComplete) {
                binding.ivComplete.visibility = android.view.View.VISIBLE
                binding.tvCompleteLabel.visibility = android.view.View.VISIBLE
            } else {
                binding.ivComplete.visibility = android.view.View.GONE
                binding.tvCompleteLabel.visibility = android.view.View.GONE
            }

            val pct = "%.0f%%".format(stats.completionPercent)
            binding.tvPercent.text = pct

            binding.root.setOnClickListener { onSetClick(stats.set.id) }
        }
    }
}

class SetDiffCallback : DiffUtil.ItemCallback<SetStats>() {
    override fun areItemsTheSame(oldItem: SetStats, newItem: SetStats) =
        oldItem.set.id == newItem.set.id
    override fun areContentsTheSame(oldItem: SetStats, newItem: SetStats) = oldItem == newItem
}
