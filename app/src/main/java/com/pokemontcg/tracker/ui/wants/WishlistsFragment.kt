package com.pokemontcg.tracker.ui.wants

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistSummary
import com.pokemontcg.tracker.databinding.FragmentWishlistsBinding
import kotlinx.coroutines.launch

class WishlistsFragment : Fragment() {

    private var _binding: FragmentWishlistsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WishlistsViewModel by viewModels {
        WishlistsViewModelFactory((requireActivity().application as PokemonApp).repository)
    }

    private lateinit var adapter: WishlistListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishlistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupActions()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = getString(R.string.nav_wants)
    }

    private fun setupRecyclerView() {
        adapter = WishlistListAdapter(
            onWishlistClick = { wishlist ->
                findNavController().navigate(
                    R.id.nav_wishlist_detail,
                    bundleOf("wishlistId" to wishlist.id)
                )
            },
            onRenameClick = { wishlist ->
                showWishlistNameDialog(
                    titleRes = R.string.wishlist_rename_title,
                    initialValue = wishlist.name,
                    onSubmit = { name -> viewModel.renameWishlist(wishlist.id, name) }
                )
            },
            onDeleteClick = { wishlist ->
                confirmDeleteWishlist(wishlist)
            }
        )

        binding.rvWishlists.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WishlistsFragment.adapter
        }
    }

    private fun setupActions() {
        binding.fabCreateWishlist.setOnClickListener { showCreateWishlistDialog() }
        binding.btnCreateFirstWishlist.setOnClickListener { showCreateWishlistDialog() }
    }

    private fun observeViewModel() {
        viewModel.wishlists.observe(viewLifecycleOwner) { wishlists ->
            adapter.submitList(wishlists)
            val isEmpty = wishlists.isEmpty()
            binding.emptyStateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvWishlists.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showCreateWishlistDialog() {
        showWishlistNameDialog(
            titleRes = R.string.wishlist_create_title,
            onSubmit = { name -> viewModel.createWishlist(name) }
        )
    }

    private fun confirmDeleteWishlist(wishlist: WishlistSummary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wishlist_delete_title)
            .setMessage(getString(R.string.wishlist_delete_message, wishlist.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteWishlist(wishlist.id)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
