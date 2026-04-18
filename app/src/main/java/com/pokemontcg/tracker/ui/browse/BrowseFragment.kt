package com.pokemontcg.tracker.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.databinding.FragmentBrowseBinding

class BrowseFragment : Fragment() {

    private var _binding: FragmentBrowseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BrowseViewModel by viewModels {
        BrowseViewModelFactory((requireActivity().application as PokemonApp).repository)
    }

    private lateinit var adapter: BrowseSetAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = BrowseSetAdapter { setId ->
            val action = BrowseFragmentDirections.actionBrowseToSetDetail(setId)
            findNavController().navigate(action)
        }
        binding.rvBrowse.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@BrowseFragment.adapter
        }
    }

    private fun setupSearch() {
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString() ?: "")
        }
    }

    private fun observeViewModel() {
        viewModel.setsBySeries.observe(viewLifecycleOwner) { grouped ->
            val items = mutableListOf<BrowseItem>()
            grouped.forEach { (series, sets) ->
                items.add(BrowseItem.Header(series))
                sets.forEach { set -> items.add(BrowseItem.SetItem(set)) }
            }
            adapter.submitList(items)

            binding.tvEmptyState.visibility =
                if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
