package com.pokemontcg.tracker.ui.card

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.pokemontcg.tracker.PokemonApp
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.databinding.FragmentCardDetailBinding
import com.pokemontcg.tracker.ui.components.loadCardAsset
import com.pokemontcg.tracker.ui.wants.showWishlistPickerDialog

class CardDetailFragment : Fragment() {

    private var _binding: FragmentCardDetailBinding? = null
    private val binding get() = _binding!!

    private val cardId: String by lazy {
        requireArguments().getString(ARG_CARD_ID).orEmpty()
    }

    private val sourceWishlistId: Long? by lazy {
        requireArguments().getLong(ARG_WISHLIST_ID).takeIf { it > 0L }
    }

    private val viewModel: CardDetailViewModel by viewModels {
        CardDetailViewModelFactory(
            (requireActivity().application as PokemonApp).repository,
            cardId,
            sourceWishlistId
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupActions() {
        binding.btnCollectionAction.setOnClickListener {
            viewModel.performCollectionAction()
        }
        binding.btnWishlistAction.setOnClickListener {
            showWishlistPickerDialog(
                cardId = cardId,
                fetchMemberships = { viewModel.getWishlistMembershipStates() },
                saveMemberships = { _, selectedIds -> viewModel.setCardWishlistMemberships(selectedIds) },
                createWishlist = { name -> viewModel.createWishlist(name) }
            )
        }
        binding.btnBackToWishlist.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeViewModel() {
        viewModel.card.observe(viewLifecycleOwner) { card ->
            if (card == null) return@observe

            requireActivity().title = card.name
            binding.ivCardScan.loadCardAsset(card.imageLarge)
            binding.tvCardName.text = card.name
            binding.tvCardSet.text = getString(R.string.card_detail_set_label, card.setName, card.number)
            binding.tvCardSeries.text = card.setSeries
            binding.tvCardRarity.text = card.rarity
            binding.tvCardSupertype.text = card.supertype
            binding.tvCardTypes.text = card.types.ifBlank { getString(R.string.card_detail_no_type) }
            binding.tvCardOwnership.text = if (card.isOwned) {
                getString(R.string.card_detail_owned, card.ownedQuantity)
            } else {
                getString(R.string.card_detail_not_owned)
            }
            binding.btnCollectionAction.text = if (card.isOwned) {
                getString(R.string.card_action_remove_from_collection)
            } else {
                getString(R.string.card_action_mark_got)
            }
            binding.btnBackToWishlist.visibility = if (sourceWishlistId != null) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBarCardDetail.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CARD_ID = "cardId"
        private const val ARG_WISHLIST_ID = "wishlistId"
    }
}
