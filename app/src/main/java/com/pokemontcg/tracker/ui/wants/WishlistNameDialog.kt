package com.pokemontcg.tracker.ui.wants

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.WishlistSaveResult
import kotlinx.coroutines.launch

fun Fragment.showWishlistNameDialog(
    @StringRes titleRes: Int,
    initialValue: String = "",
    onSubmit: suspend (String) -> WishlistSaveResult,
    onSuccess: (Long) -> Unit = {}
) {
    val context = requireContext()
    val density = resources.displayMetrics.density
    val margin = (20 * density).toInt()

    val textInputLayout = TextInputLayout(context).apply {
        hint = getString(R.string.wishlist_name_hint)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val editText = TextInputEditText(textInputLayout.context).apply {
        setText(initialValue)
        setSelection(text?.length ?: 0)
        doAfterTextChanged { textInputLayout.error = null }
    }
    textInputLayout.addView(editText)

    val container = FrameLayout(context).apply {
        setPadding(margin, margin / 2, margin, 0)
        addView(textInputLayout)
    }

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(titleRes)
        .setView(container)
        .setPositiveButton(R.string.action_save, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = onSubmit(editText.text?.toString().orEmpty())) {
                    WishlistSaveResult.BlankName -> {
                        textInputLayout.error = getString(R.string.wishlist_error_blank_name)
                    }
                    WishlistSaveResult.DuplicateName -> {
                        textInputLayout.error = getString(R.string.wishlist_error_duplicate_name)
                    }
                    is WishlistSaveResult.Success -> {
                        dialog.dismiss()
                        onSuccess(result.wishlistId)
                    }
                }
            }
        }
    }

    dialog.show()
}
