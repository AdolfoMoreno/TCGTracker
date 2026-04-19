package com.pokemontcg.tracker.ui.home

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.pokemontcg.tracker.R
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.databinding.FragmentSetDetailBinding
import com.pokemontcg.tracker.ui.components.CardGridAdapter

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
        adapter = CardGridAdapter { cardId ->
            viewModel.toggleCard(cardId)
        }
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
}
