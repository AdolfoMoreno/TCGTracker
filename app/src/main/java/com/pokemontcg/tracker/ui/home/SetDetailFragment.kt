package com.pokemontcg.tracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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
    }

    private fun setupFilters() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilterOwned(false)
            binding.chipAll.isChecked = true
            binding.chipOwned.isChecked = false
        }
        binding.chipOwned.setOnClickListener {
            viewModel.setFilterOwned(true)
            binding.chipOwned.isChecked = true
            binding.chipAll.isChecked = false
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
