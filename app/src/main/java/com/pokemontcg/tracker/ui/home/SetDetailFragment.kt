package com.pokemontcg.tracker.ui.home

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.pokemontcg.tracker.R
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.databinding.FragmentSetDetailBinding
import com.pokemontcg.tracker.ui.components.CardGridAdapter
import com.pokemontcg.tracker.ui.wants.showWishlistNameDialog
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
                viewModel.toggleCard(cardId)
            },
            onCardLongClick = { cardId ->
                showWishlistPicker(cardId)
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

    private fun showWishlistPicker(cardId: String, selectedOverride: Set<Long>? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val memberships = viewModel.getWishlistMembershipStates(cardId)
            if (memberships.isEmpty()) {
                showCreateWishlistForCard(cardId)
                return@launch
            }

            val wishlistNames = memberships.map { it.name }.toTypedArray()
            val checkedState = BooleanArray(memberships.size) { index ->
                selectedOverride?.contains(memberships[index].id) ?: memberships[index].isSelected
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.wishlist_picker_title)
                .setMultiChoiceItems(wishlistNames, checkedState) { _, which, isChecked ->
                    checkedState[which] = isChecked
                }
                .setPositiveButton(R.string.action_save) { _, _ ->
                    val selectedIds = memberships.indices
                        .filter { checkedState[it] }
                        .map { memberships[it].id }
                        .toSet()

                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.setCardWishlistMemberships(cardId, selectedIds)
                        Toast.makeText(
                            requireContext(),
                            R.string.wishlist_picker_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.action_create_wishlist) { _, _ ->
                    val pendingIds = memberships.indices
                        .filter { checkedState[it] }
                        .map { memberships[it].id }
                        .toSet()
                    showCreateWishlistForCard(cardId, pendingIds)
                }
                .show()
        }
    }

    private fun showCreateWishlistForCard(cardId: String, selectedIds: Set<Long> = emptySet()) {
        showWishlistNameDialog(
            titleRes = R.string.wishlist_create_title,
            onSubmit = { name -> viewModel.createWishlist(name) },
            onSuccess = { wishlistId ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setCardWishlistMemberships(cardId, selectedIds + wishlistId)
                    Toast.makeText(
                        requireContext(),
                        R.string.wishlist_picker_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
