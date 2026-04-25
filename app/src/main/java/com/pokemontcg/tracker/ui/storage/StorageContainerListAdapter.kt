package com.pokemontcg.tracker.ui.storage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.StorageContainerSummary
import com.pokemontcg.tracker.databinding.ItemStorageContainerBinding

class StorageContainerListAdapter(
    private val onContainerClick: (StorageContainerSummary) -> Unit,
    private val onRenameClick: (StorageContainerSummary) -> Unit,
    private val onDeleteClick: (StorageContainerSummary) -> Unit
) : ListAdapter<StorageContainerSummary, StorageContainerListAdapter.StorageContainerViewHolder>(
    StorageContainerDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StorageContainerViewHolder {
        val binding = ItemStorageContainerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StorageContainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StorageContainerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StorageContainerViewHolder(
        private val binding: ItemStorageContainerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: StorageContainerSummary) {
            binding.tvContainerName.text = item.name
            binding.tvContainerType.text = binding.root.context.getString(
                if (item.type == com.pokemontcg.tracker.data.model.StorageContainerType.BINDER) {
                    R.string.storage_type_binder
                } else {
                    R.string.storage_type_box
                }
            )
            binding.tvContainerMeta.text = binding.root.context.getString(
                R.string.storage_row_meta,
                item.usedCapacity,
                item.capacity,
                item.storedCardCount
            )
            binding.tvContainerHint.visibility =
                if (item.storedCardCount == 0) View.VISIBLE else View.GONE

            binding.root.setOnClickListener { onContainerClick(item) }
            binding.btnContainerMenu.setOnClickListener { anchor ->
                PopupMenu(anchor.context, anchor).apply {
                    inflate(R.menu.storage_container_item_menu)
                    setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_rename_storage_container -> {
                                onRenameClick(item)
                                true
                            }
                            R.id.action_delete_storage_container -> {
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

private class StorageContainerDiffCallback : DiffUtil.ItemCallback<StorageContainerSummary>() {
    override fun areItemsTheSame(
        oldItem: StorageContainerSummary,
        newItem: StorageContainerSummary
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: StorageContainerSummary,
        newItem: StorageContainerSummary
    ): Boolean = oldItem == newItem
}
