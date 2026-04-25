package com.pokemontcg.tracker.ui.storage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.StorageAssignmentResult
import com.pokemontcg.tracker.data.model.StorageCardItem
import com.pokemontcg.tracker.databinding.FragmentStorageContainerDetailBinding
import kotlinx.coroutines.launch

class StorageContainerDetailFragment : Fragment() {

    private var _binding: FragmentStorageContainerDetailBinding? = null
    private val binding get() = _binding!!

    private val containerId: Long by lazy {
        requireArguments().getLong(ARG_CONTAINER_ID)
    }

    private val viewModel: StorageContainerDetailViewModel by viewModels {
        StorageContainerDetailViewModelFactory(
            (requireActivity().application as PokemonApp).repository,
            containerId
        )
    }

    private lateinit var adapter: StorageCardGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageContainerDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupRecyclerView() {
        adapter = StorageCardGridAdapter(
            onCardClick = { card -> openCardDetail(card.id) },
            onCardLongClick = { card -> showCardActions(card) }
        )
        binding.rvStorageCards.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = this@StorageContainerDetailFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.container.observe(viewLifecycleOwner) { container ->
            requireActivity().title = container?.name ?: getString(R.string.storage_detail_title)
            updateSummary()
        }

        viewModel.cards.observe(viewLifecycleOwner) { cards ->
            adapter.submitList(cards)
            updateSummary()
            val isEmpty = cards.isEmpty()
            binding.tvStorageEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvStorageCards.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarStorage.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showCardActions(card: StorageCardItem) {
        val options = arrayOf(
            getString(R.string.storage_action_add_more),
            getString(R.string.storage_action_remove_quantity),
            getString(R.string.card_action_open_details)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(card.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> addCopies(card)
                    1 -> removeCopies(card)
                    2 -> openCardDetail(card.id)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addCopies(card: StorageCardItem) {
        viewLifecycleOwner.lifecycleScope.launch {
            val summary = viewModel.getCardStorageSummary(card.id)
            val remainingCapacity = ((viewModel.container.value?.capacity ?: 0)
                - (viewModel.cards.value?.sumOf { it.storedQuantity } ?: 0)).coerceAtLeast(0)
            val maxAssignable = minOf(summary.availableToAssign, remainingCapacity)
            if (maxAssignable <= 0) {
                Toast.makeText(
                    requireContext(),
                    if (summary.availableToAssign <= 0) {
                        R.string.storage_error_no_available_copies
                    } else {
                        R.string.storage_error_container_full
                    },
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            showStorageQuantityDialog(
                titleRes = R.string.storage_assign_quantity_title,
                maxQuantity = maxAssignable
            ) { quantity ->
                viewLifecycleOwner.lifecycleScope.launch {
                    showStorageResult(viewModel.addCopies(card.id, quantity))
                }
            }
        }
    }

    private fun removeCopies(card: StorageCardItem) {
        showStorageQuantityDialog(
            titleRes = R.string.storage_remove_quantity_title,
            maxQuantity = card.storedQuantity,
            initialQuantity = 1
        ) { quantity ->
            viewLifecycleOwner.lifecycleScope.launch {
                showStorageResult(viewModel.removeCopies(card.id, quantity))
            }
        }
    }

    private fun showStorageResult(result: StorageAssignmentResult) {
        val messageRes = when (result) {
            StorageAssignmentResult.Success -> R.string.storage_assignment_updated
            StorageAssignmentResult.NoAvailableCopies -> R.string.storage_error_no_available_copies
            StorageAssignmentResult.ContainerFull -> R.string.storage_error_container_full
            StorageAssignmentResult.NotOwned -> R.string.storage_error_not_owned
            StorageAssignmentResult.AssignmentMissing -> R.string.storage_error_assignment_missing
            else -> R.string.storage_error_invalid_quantity_simple
        }
        Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
    }

    private fun openCardDetail(cardId: String) {
        findNavController().navigate(
            R.id.nav_card_detail,
            bundleOf("cardId" to cardId, "wishlistId" to -1L)
        )
    }

    private fun updateSummary() {
        val container = viewModel.container.value ?: return
        val usedCapacity = viewModel.cards.value?.sumOf { it.storedQuantity } ?: 0
        binding.tvStorageSummary.text = getString(
            R.string.storage_detail_summary,
            if (container.type == com.pokemontcg.tracker.data.model.StorageContainerType.BINDER) {
                getString(R.string.storage_type_binder)
            } else {
                getString(R.string.storage_type_box)
            },
            usedCapacity,
            container.capacity
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CONTAINER_ID = "containerId"
    }
}
