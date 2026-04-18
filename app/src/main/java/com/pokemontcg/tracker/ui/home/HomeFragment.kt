package com.pokemontcg.tracker.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.databinding.FragmentHomeBinding
import com.pokemontcg.tracker.ui.components.SetCollectionAdapter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory((requireActivity().application as PokemonApp).repository)
    }

    private lateinit var adapter: SetCollectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SetCollectionAdapter { setId ->
            val action = HomeFragmentDirections.actionHomeToSetDetail(setId)
            findNavController().navigate(action)
        }
        binding.rvSets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            binding.tvOwnedCount.text = stats.ownedCards.toString()
            binding.tvTotalCount.text = stats.totalCards.toString()
            binding.tvCompletedSets.text = stats.completedSets.toString()
            binding.tvTotalSets.text = stats.totalSets.toString()
        }

        viewModel.setStats.observe(viewLifecycleOwner) { sets ->
            adapter.submitList(sets)
            binding.tvTotalSets.text = sets.size.toString()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.contentGroup.visibility = if (loading) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshCollection()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
