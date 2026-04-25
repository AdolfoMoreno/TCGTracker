package com.pokemontcg.tracker.ui.wants

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistMembershipState
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import kotlinx.coroutines.launch

fun Fragment.showWishlistPickerDialog(
    cardId: String,
    fetchMemberships: suspend (String) -> List<WishlistMembershipState>,
    saveMemberships: suspend (String, Set<Long>) -> Unit,
    createWishlist: suspend (String) -> WishlistSaveResult
) {
    fun openCreateDialog(selectedIds: Set<Long>) {
        showWishlistNameDialog(
            titleRes = R.string.wishlist_create_title,
            onSubmit = createWishlist,
            onSuccess = { wishlistId ->
                viewLifecycleOwner.lifecycleScope.launch {
                    saveMemberships(cardId, selectedIds + wishlistId)
                    Toast.makeText(
                        requireContext(),
                        R.string.wishlist_picker_saved,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    fun openPicker(selectedOverride: Set<Long>? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val memberships = fetchMemberships(cardId)
            if (memberships.isEmpty()) {
                openCreateDialog(emptySet())
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
                        saveMemberships(cardId, selectedIds)
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
                    openCreateDialog(pendingIds)
                }
                .show()
        }
    }

    openPicker()
}
