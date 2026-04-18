package com.pokemontcg.tracker.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.CardWithCollection
import com.pokemontcg.tracker.databinding.ItemCardGridBinding

class CardGridAdapter(
    private val onCardClick: (String) -> Unit
) : ListAdapter<CardWithCollection, CardGridAdapter.CardViewHolder>(CardDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val binding = ItemCardGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CardViewHolder(
        private val binding: ItemCardGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CardWithCollection) {
            val card = item.card
            binding.tvCardNumber.text = "#${card.number}"
            binding.tvCardName.text = card.name
            binding.tvRarity.text = card.rarity
            binding.tvSupertype.text = card.supertype

            // Type color dot
            val typeColor = getTypeColor(card.types)
            binding.viewTypeDot.setBackgroundColor(
                ContextCompat.getColor(binding.root.context, typeColor)
            )

            // Owned state styling
            if (item.isOwned) {
                binding.root.alpha = 1.0f
                binding.cardView.strokeWidth = 4
                binding.cardView.strokeColor =
                    ContextCompat.getColor(binding.root.context, R.color.owned_stroke)
                binding.ivCheckmark.visibility = android.view.View.VISIBLE
                binding.tvQuantity.text = "×${item.quantity}"
                binding.tvQuantity.visibility = android.view.View.VISIBLE
            } else {
                binding.root.alpha = 0.5f
                binding.cardView.strokeWidth = 0
                binding.ivCheckmark.visibility = android.view.View.GONE
                binding.tvQuantity.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener { onCardClick(card.id) }
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

class CardDiffCallback : DiffUtil.ItemCallback<CardWithCollection>() {
    override fun areItemsTheSame(oldItem: CardWithCollection, newItem: CardWithCollection) =
        oldItem.card.id == newItem.card.id
    override fun areContentsTheSame(oldItem: CardWithCollection, newItem: CardWithCollection) =
        oldItem == newItem
}
