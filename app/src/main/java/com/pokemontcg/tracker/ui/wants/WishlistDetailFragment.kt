package com.pokemontcg.tracker.ui.wants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistCardItem
import com.pokemontcg.tracker.databinding.FragmentWishlistDetailBinding

class WishlistDetailFragment : Fragment() {

    private var _binding: FragmentWishlistDetailBinding? = null
    private val binding get() = _binding!!

    private val wishlistId: Long by lazy {
        requireArguments().getLong(ARG_WISHLIST_ID)
    }

    private val viewModel: WishlistDetailViewModel by viewModels {
        WishlistDetailViewModelFactory(
            (requireActivity().application as PokemonApp).repository,
            wishlistId
        )
    }

    private lateinit var adapter: WishlistCardGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistDetailBinding.inflate(inflater, container, false)
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
        adapter = WishlistCardGridAdapter { card ->
            showCardActions(card)
        }
        binding.rvWishlistCards.apply {
            layoutManager = GridLayoutManager(requireContext(), 5)
            adapter = this@WishlistDetailFragment.adapter
        }
    }

    private fun observeViewModel() {
        viewModel.wishlist.observe(viewLifecycleOwner) { wishlist ->
            requireActivity().title = wishlist?.name ?: getString(R.string.wishlist_detail_title)
        }

        viewModel.cards.observe(viewLifecycleOwner) { cards ->
            adapter.submitList(cards)
            binding.tvWishlistSummary.text = getString(
                R.string.wishlist_detail_summary,
                cards.count { it.isOwned },
                cards.size
            )
            val isEmpty = cards.isEmpty()
            binding.tvWishlistEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvWishlistCards.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarWishlist.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun showCardActions(card: WishlistCardItem) {
        val options = arrayOf(
            getString(R.string.wishlist_action_mark_got),
            getString(R.string.wishlist_action_remove)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(card.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.markCardAsCollected(card.id)
                    1 -> viewModel.removeCard(card.id)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_WISHLIST_ID = "wishlistId"
    }
}
