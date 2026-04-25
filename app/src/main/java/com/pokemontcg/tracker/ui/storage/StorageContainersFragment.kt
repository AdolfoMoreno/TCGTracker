package com.pokemontcg.tracker.ui.storage

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
import com.pokemontcg.tracker.data.model.StorageContainerSummary
import com.pokemontcg.tracker.databinding.FragmentStorageContainersBinding
import kotlinx.coroutines.launch

class StorageContainersFragment : Fragment() {

    private var _binding: FragmentStorageContainersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StorageContainersViewModel by viewModels {
        StorageContainersViewModelFactory((requireActivity().application as PokemonApp).repository)
    }

    private lateinit var adapter: StorageContainerListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStorageContainersBinding.inflate(inflater, container, false)
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
        requireActivity().title = getString(R.string.nav_storage)
    }

    private fun setupRecyclerView() {
        adapter = StorageContainerListAdapter(
            onContainerClick = { container ->
                findNavController().navigate(
                    R.id.nav_storage_container_detail,
                    bundleOf("containerId" to container.id)
                )
            },
            onRenameClick = { container ->
                showStorageContainerRenameDialog(
                    initialValue = container.name,
                    onSubmit = { name -> viewModel.renameContainer(container.id, name) }
                )
            },
            onDeleteClick = { container ->
                confirmDeleteContainer(container)
            }
        )

        binding.rvStorageContainers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@StorageContainersFragment.adapter
        }
    }

    private fun setupActions() {
        binding.fabCreateContainer.setOnClickListener { showCreateContainerDialog() }
        binding.btnCreateFirstContainer.setOnClickListener { showCreateContainerDialog() }
    }

    private fun observeViewModel() {
        viewModel.containers.observe(viewLifecycleOwner) { containers ->
            adapter.submitList(containers)
            val isEmpty = containers.isEmpty()
            binding.emptyStateGroup.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.rvStorageContainers.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }
    }

    private fun showCreateContainerDialog() {
        showStorageContainerCreateDialog(
            onSubmit = { name, type, capacity -> viewModel.createContainer(name, type, capacity) }
        )
    }

    private fun confirmDeleteContainer(container: StorageContainerSummary) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.storage_delete_title)
            .setMessage(getString(R.string.storage_delete_message, container.name))
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteContainer(container.id)
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
