package com.pokemontcg.tracker.ui.wants

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistSummary
import com.pokemontcg.tracker.databinding.ItemWishlistBinding

class WishlistListAdapter(
    private val onWishlistClick: (WishlistSummary) -> Unit,
    private val onRenameClick: (WishlistSummary) -> Unit,
    private val onDeleteClick: (WishlistSummary) -> Unit
) : ListAdapter<WishlistSummary, WishlistListAdapter.WishlistViewHolder>(WishlistSummaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WishlistViewHolder(
        private val binding: ItemWishlistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WishlistSummary) {
            binding.tvWishlistName.text = item.name
            binding.tvWishlistMeta.text = binding.root.context.getString(
                R.string.wishlist_row_meta,
                item.cardCount,
                item.ownedCount
            )
            binding.tvWishlistHint.visibility =
                if (item.cardCount == 0) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onWishlistClick(item) }
            binding.btnWishlistMenu.setOnClickListener { anchor ->
                PopupMenu(anchor.context, anchor).apply {
                    inflate(R.menu.wishlist_item_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_rename_wishlist -> {
                                onRenameClick(item)
                                true
                            }
                            R.id.action_delete_wishlist -> {
                                onDeleteClick(item)
                                true
                            }
                            else -> false
                        }
                    }
                }.show()
            }
        }
    }
}

private class WishlistSummaryDiffCallback : DiffUtil.ItemCallback<WishlistSummary>() {
    override fun areItemsTheSame(oldItem: WishlistSummary, newItem: WishlistSummary): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: WishlistSummary, newItem: WishlistSummary): Boolean =
        oldItem == newItem
}
