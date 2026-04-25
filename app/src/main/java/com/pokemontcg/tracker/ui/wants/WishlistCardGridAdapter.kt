package com.pokemontcg.tracker.ui.wants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistCardItem
import com.pokemontcg.tracker.databinding.ItemCardGridBinding

class WishlistCardGridAdapter(
    private val onCardClick: (WishlistCardItem) -> Unit
) : ListAdapter<WishlistCardItem, WishlistCardGridAdapter.WishlistCardViewHolder>(WishlistCardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistCardViewHolder {
        val binding = ItemCardGridBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WishlistCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistCardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WishlistCardViewHolder(
        private val binding: ItemCardGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WishlistCardItem) {
            binding.tvCardNumber.text = "#${item.number}"
            binding.tvCardName.text = item.name
            binding.tvCardSet.text = item.setName
            binding.tvCardSet.visibility = View.VISIBLE
            binding.tvRarity.text = item.rarity
            binding.tvSupertype.text = item.supertype

            val typeColor = getTypeColor(item.types)
            binding.viewTypeDot.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )

            if (item.isOwned) {
                binding.root.alpha = 1.0f
                binding.cardView.strokeWidth = 4
                binding.cardView.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.owned_stroke)
                binding.ivCheckmark.visibility = View.VISIBLE
                binding.tvQuantity.text = "×${item.ownedQuantity}"
                binding.tvQuantity.visibility = View.VISIBLE
            } else {
                binding.root.alpha = 0.6f
                binding.cardView.strokeWidth = 0
                binding.ivCheckmark.visibility = View.GONE
                binding.tvQuantity.visibility = View.GONE
            }

            binding.root.setOnClickListener { onCardClick(item) }
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

private class WishlistCardDiffCallback : DiffUtil.ItemCallback<WishlistCardItem>() {
    override fun areItemsTheSame(oldItem: WishlistCardItem, newItem: WishlistCardItem): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: WishlistCardItem, newItem: WishlistCardItem): Boolean =
        oldItem == newItem
}
