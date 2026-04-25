package com.pokemontcg.tracker.ui.storage

import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.data.model.CardStorageSummary
import com.pokemontcg.tracker.data.model.StorageAssignmentResult
import com.pokemontcg.tracker.data.model.StorageContainerOption
import com.pokemontcg.tracker.data.model.StorageContainerSaveResult
import com.pokemontcg.tracker.data.model.StorageContainerType
import kotlinx.coroutines.launch

fun Fragment.storageTypeLabel(type: String): String {
    return when (type) {
        StorageContainerType.BINDER -> getString(R.string.storage_type_binder)
        StorageContainerType.BOX -> getString(R.string.storage_type_box)
        else -> type.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun Fragment.showStorageContainerCreateDialog(
    onSubmit: suspend (name: String, type: String, capacity: Int) -> StorageContainerSaveResult,
    onSuccess: (Long) -> Unit = {}
) {
    val context = requireContext()
    val density = resources.displayMetrics.density
    val padding = (20 * density).toInt()

    val root = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(padding, padding / 2, padding, 0)
    }

    val typeGroup = RadioGroup(context).apply {
        orientation = RadioGroup.HORIZONTAL
    }
    val binderButton = RadioButton(context).apply {
        id = android.view.View.generateViewId()
        text = getString(R.string.storage_type_binder)
        isChecked = true
    }
    val boxButton = RadioButton(context).apply {
        id = android.view.View.generateViewId()
        text = getString(R.string.storage_type_box)
    }
    typeGroup.addView(binderButton)
    typeGroup.addView(boxButton)

    val nameLayout = TextInputLayout(context).apply {
        hint = getString(R.string.storage_name_hint)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    val nameEditText = TextInputEditText(context).apply {
        doAfterTextChanged { nameLayout.error = null }
    }
    nameLayout.addView(nameEditText)

    val sizeLayout = TextInputLayout(context).apply {
        hint = getString(R.string.storage_capacity_hint)
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
    val sizeEditText = TextInputEditText(context).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        doAfterTextChanged { sizeLayout.error = null }
    }
    sizeLayout.addView(sizeEditText)

    root.addView(typeGroup)
    root.addView(nameLayout)
    root.addView(sizeLayout)

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(R.string.storage_create_title)
        .setView(root)
        .setPositiveButton(R.string.action_save, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val selectedType = when (typeGroup.checkedRadioButtonId) {
                    boxButton.id -> StorageContainerType.BOX
                    else -> StorageContainerType.BINDER
                }
                val capacity = sizeEditText.text?.toString()?.trim()?.toIntOrNull() ?: -1
                when (val result = onSubmit(nameEditText.text?.toString().orEmpty(), selectedType, capacity)) {
                    StorageContainerSaveResult.BlankName -> {
                        nameLayout.error = getString(R.string.storage_error_blank_name)
                    }
                    StorageContainerSaveResult.DuplicateName -> {
                        nameLayout.error = getString(R.string.storage_error_duplicate_name)
                    }
                    StorageContainerSaveResult.InvalidCapacity -> {
                        sizeLayout.error = getString(R.string.storage_error_invalid_capacity)
                    }
                    is StorageContainerSaveResult.Success -> {
                        dialog.dismiss()
                        onSuccess(result.containerId)
                    }
                }
            }
        }
    }

    dialog.show()
}

fun Fragment.showStorageContainerRenameDialog(
    initialValue: String,
    onSubmit: suspend (String) -> StorageContainerSaveResult,
    onSuccess: (Long) -> Unit = {}
) {
    val context = requireContext()
    val density = resources.displayMetrics.density
    val margin = (20 * density).toInt()

    val textInputLayout = TextInputLayout(context).apply {
        hint = getString(R.string.storage_name_hint)
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
        .setTitle(R.string.storage_rename_title)
        .setView(container)
        .setPositiveButton(R.string.action_save, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = onSubmit(editText.text?.toString().orEmpty())) {
                    StorageContainerSaveResult.BlankName -> {
                        textInputLayout.error = getString(R.string.storage_error_blank_name)
                    }
                    StorageContainerSaveResult.DuplicateName -> {
                        textInputLayout.error = getString(R.string.storage_error_duplicate_name)
                    }
                    StorageContainerSaveResult.InvalidCapacity -> {
                        textInputLayout.error = getString(R.string.storage_error_invalid_capacity)
                    }
                    is StorageContainerSaveResult.Success -> {
                        dialog.dismiss()
                        onSuccess(result.containerId)
                    }
                }
            }
        }
    }

    dialog.show()
}

fun Fragment.showStorageQuantityDialog(
    @StringRes titleRes: Int,
    maxQuantity: Int,
    initialQuantity: Int = 1,
    onConfirm: (Int) -> Unit
) {
    val context = requireContext()
    val density = resources.displayMetrics.density
    val margin = (20 * density).toInt()

    val inputLayout = TextInputLayout(context).apply {
        hint = getString(R.string.storage_quantity_hint)
        helperText = getString(R.string.storage_quantity_max, maxQuantity)
    }
    val input = TextInputEditText(context).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(initialQuantity.toString())
        setSelection(text?.length ?: 0)
        doAfterTextChanged { inputLayout.error = null }
    }
    inputLayout.addView(input)

    val container = FrameLayout(context).apply {
        setPadding(margin, margin / 2, margin, 0)
        addView(inputLayout)
    }

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(titleRes)
        .setView(container)
        .setPositiveButton(R.string.action_save, null)
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val quantity = input.text?.toString()?.trim()?.toIntOrNull()
            if (quantity == null || quantity <= 0 || quantity > maxQuantity) {
                inputLayout.error = getString(R.string.storage_error_invalid_quantity, maxQuantity)
            } else {
                dialog.dismiss()
                onConfirm(quantity)
            }
        }
    }

    dialog.show()
}

fun Fragment.showStoragePickerDialog(
    cardName: String,
    fetchOptions: suspend () -> List<StorageContainerOption>,
    fetchSummary: suspend () -> CardStorageSummary,
    assignToContainer: suspend (containerId: Long, quantity: Int) -> StorageAssignmentResult
) {
    viewLifecycleOwner.lifecycleScope.launch {
        val summary = fetchSummary()
        if (summary.availableToAssign <= 0) {
            Toast.makeText(
                requireContext(),
                R.string.storage_error_no_available_copies,
                Toast.LENGTH_SHORT
            ).show()
            return@launch
        }

        val options = fetchOptions()
            .filter { it.remainingCapacity > 0 }
        if (options.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.storage_error_no_available_containers,
                Toast.LENGTH_SHORT
            ).show()
            return@launch
        }

        val optionLabels = options.map {
            getString(
                R.string.storage_picker_option,
                it.name,
                storageTypeLabel(it.type),
                it.remainingCapacity
            )
        }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.storage_picker_title, cardName))
            .setItems(optionLabels) { _, which ->
                val option = options[which]
                val maxAssignable = minOf(summary.availableToAssign, option.remainingCapacity)
                showStorageQuantityDialog(
                    titleRes = R.string.storage_assign_quantity_title,
                    maxQuantity = maxAssignable
                ) { quantity ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        when (assignToContainer(option.id, quantity)) {
                            StorageAssignmentResult.Success -> Toast.makeText(
                                requireContext(),
                                getString(R.string.storage_assignment_saved, option.name),
                                Toast.LENGTH_SHORT
                            ).show()

                            StorageAssignmentResult.NoAvailableCopies -> Toast.makeText(
                                requireContext(),
                                R.string.storage_error_no_available_copies,
                                Toast.LENGTH_SHORT
                            ).show()

                            StorageAssignmentResult.ContainerFull -> Toast.makeText(
                                requireContext(),
                                R.string.storage_error_container_full,
                                Toast.LENGTH_SHORT
                            ).show()

                            StorageAssignmentResult.NotOwned -> Toast.makeText(
                                requireContext(),
                                R.string.storage_error_not_owned,
                                Toast.LENGTH_SHORT
                            ).show()

                            else -> Toast.makeText(
                                requireContext(),
                                R.string.storage_error_invalid_quantity_simple,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
