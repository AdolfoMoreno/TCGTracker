package com.pokemontcg.tracker.ui.sections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.pokemontcg.tracker.R
import com.pokemontcg.tracker.databinding.FragmentSectionPlaceholderBinding

class SectionPlaceholderFragment : Fragment() {

    private var _binding: FragmentSectionPlaceholderBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSectionPlaceholderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val destinationId = findNavController().currentDestination?.id
        val content = when (destinationId) {
            R.id.nav_storage -> SectionContent(
                titleRes = R.string.nav_storage,
                kickerRes = R.string.section_storage_kicker,
                summaryRes = R.string.section_storage_summary,
                nextStepRes = R.string.section_storage_next
            )
            R.id.nav_wants -> SectionContent(
                titleRes = R.string.nav_wants,
                kickerRes = R.string.section_wants_kicker,
                summaryRes = R.string.section_wants_summary,
                nextStepRes = R.string.section_wants_next
            )
            R.id.nav_decks -> SectionContent(
                titleRes = R.string.nav_decks,
                kickerRes = R.string.section_decks_kicker,
                summaryRes = R.string.section_decks_summary,
                nextStepRes = R.string.section_decks_next
            )
            else -> SectionContent(
                titleRes = R.string.nav_settings,
                kickerRes = R.string.section_settings_kicker,
                summaryRes = R.string.section_settings_summary,
                nextStepRes = R.string.section_settings_next
            )
        }

        binding.tvSectionTitle.setText(content.titleRes)
        binding.tvSectionKicker.setText(content.kickerRes)
        binding.tvSectionSummary.setText(content.summaryRes)
        binding.tvSectionNext.setText(content.nextStepRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private data class SectionContent(
    val titleRes: Int,
    val kickerRes: Int,
    val summaryRes: Int,
    val nextStepRes: Int
)
