package com.pokemontcg.tracker.ui.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.StorageCardItem
import com.pokemontcg.tracker.databinding.ItemCardGridBinding
import com.pokemontcg.tracker.ui.components.loadCardAsset

class StorageCardGridAdapter(
    private val onCardClick: (StorageCardItem) -> Unit,
    private val onCardLongClick: ((StorageCardItem) -> Unit)? = null
) : ListAdapter<StorageCardItem, StorageCardGridAdapter.StorageCardViewHolder>(StorageCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageCardViewHolder {
        val binding = ItemCardGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StorageCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StorageCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StorageCardViewHolder(
        private val binding: ItemCardGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StorageCardItem) {
            binding.tvCardNumber.text = "#${item.number}"
            binding.tvCardName.text = item.name
            binding.tvCardSet.text = item.setName
            binding.tvCardSet.visibility = View.VISIBLE
            binding.tvRarity.text = item.rarity
            binding.tvSupertype.text = item.supertype
            binding.ivCardArt.loadCardAsset(item.imageSmall)

            val typeColor = getTypeColor(item.types)
            binding.viewTypeDot.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )

            binding.root.alpha = 1.0f
            binding.cardView.strokeWidth = 4
            binding.cardView.strokeColor =
                ContextCompat.getColor(binding.root.context, R.color.owned_stroke)
            binding.ivCheckmark.visibility = View.VISIBLE
            binding.tvQuantity.text = "×${item.storedQuantity}"
            binding.tvQuantity.visibility = View.VISIBLE

            binding.root.setOnClickListener { onCardClick(item) }
            binding.root.setOnLongClickListener {
                onCardLongClick?.invoke(item)
                onCardLongClick != null
            }
        }

        private fun getTypeColor(types: String): Int = when {
            types.contains("Fire") -> R.color.type_fire
            types.contains("Water") -> R.color.type_water
            types.contains("Grass") -> R.color.type_grass
            types.contains("Lightning") -> R.color.type_lightning
            types.contains("Psychic") -> R.color.type_psychic
            types.contains("Fighting") -> R.color.type_fighting
            types.contains("Darkness") -> R.color.type_darkness
            types.contains("Metal") -> R.color.type_metal
            types.contains("Dragon") -> R.color.type_dragon
            else -> R.color.type_colorless
        }
    }
}

private class StorageCardDiffCallback : DiffUtil.ItemCallback<StorageCardItem>() {
    override fun areItemsTheSame(oldItem: StorageCardItem, newItem: StorageCardItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: StorageCardItem, newItem: StorageCardItem): Boolean =
        oldItem == newItem
}
