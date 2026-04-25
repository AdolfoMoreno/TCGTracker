package com.pokemontcg.tracker.ui.home

import android.os.Bundle
import androidx.core.os.bundleOf
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pokemontcg.tracker.R
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.databinding.FragmentSetDetailBinding
import com.pokemontcg.tracker.ui.components.CardGridAdapter
import com.pokemontcg.tracker.ui.wants.showWishlistPickerDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class SetDetailFragment : Fragment() {

    private var _binding: FragmentSetDetailBinding? = null
    private val binding get() = _binding!!

    private val args: SetDetailFragmentArgs by navArgs()

    private val viewModel: SetDetailViewModel by viewModels {
        SetDetailViewModelFactory(
            (requireActivity().application as PokemonApp).repository,
            args.setId
        )
    }

    private lateinit var adapter: CardGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSetDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupRecyclerView()
        setupFilters()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CardGridAdapter(
            onCardClick = { cardId ->
                openCardDetail(cardId)
            },
            onCardLongClick = { cardId ->
                showCardActions(cardId)
            }
        )
        binding.rvCards.apply {
            // Landscape grid: more columns
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = this@SetDetailFragment.adapter
        }
        viewModel.cards.observe(viewLifecycleOwner) {
            requireActivity().invalidateMenu()
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilterOwned(false)
            }
        }
        binding.chipOwned.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setFilterOwned(true)
            }
        }
        binding.chipAll.isChecked = true
    }

    private fun observeViewModel() {
        viewModel.setStats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe
            binding.tvSetTitle.text = stats.set.name
            binding.tvSetProgress.text = "${stats.ownedCount} / ${stats.totalCount} cards"
            binding.progressBar.max = stats.totalCount
            binding.progressBar.progress = stats.ownedCount
            binding.tvPercent.text = "%.0f%%".format(stats.completionPercent)
        }

        viewModel.filteredCards.observe(viewLifecycleOwner) { cards ->
            adapter.submitList(cards)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                val allOwned = viewModel.cards.value?.all { it.isOwned } ?: false
                val selectAllItem = menu.findItem(R.id.action_select_all)
                selectAllItem?.setIcon(if (allOwned) R.drawable.ic_check_circle else R.drawable.circle_dot)
                selectAllItem?.title = if (allOwned) "Deselect All" else "Select All"
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.set_detail_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_select_all -> {
                        val allOwned = viewModel.cards.value?.all { it.isOwned } ?: false
                        viewModel.selectAllCards(!allOwned)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showCardActions(cardId: String) {
        val card = viewModel.cards.value?.firstOrNull { it.card.id == cardId } ?: return
        val collectionAction = if (card.isOwned) {
            getString(R.string.card_action_remove_from_collection)
        } else {
            getString(R.string.card_action_mark_got)
        }
        val options = arrayOf(
            getString(R.string.card_action_add_to_wishlist),
            collectionAction,
            getString(R.string.card_action_open_details)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(card.card.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showWishlistPicker(cardId)
                    1 -> {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.toggleCollectionFromDetail(cardId)
                            Toast.makeText(
                                requireContext(),
                                if (card.isOwned) R.string.card_removed_from_collection else R.string.card_marked_as_got,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    2 -> openCardDetail(cardId)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showWishlistPicker(cardId: String) {
        showWishlistPickerDialog(
            cardId = cardId,
            fetchMemberships = { id -> viewModel.getWishlistMembershipStates(id) },
            saveMemberships = { id, selectedIds -> viewModel.setCardWishlistMemberships(id, selectedIds) },
            createWishlist = { name -> viewModel.createWishlist(name) }
        )
    }

    private fun openCardDetail(cardId: String) {
        findNavController().navigate(
            R.id.nav_card_detail,
            bundleOf("cardId" to cardId, "wishlistId" to -1L)
        )
    }
}
