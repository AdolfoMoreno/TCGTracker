package com.pokemontcg.tracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.navigateUp
import com.pokemontcg.tracker.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val topLevelDestinations = setOf(
        R.id.nav_my_collection,
        R.id.nav_all,
        R.id.nav_storage,
        R.id.nav_wants,
        R.id.nav_decks,
        R.id.nav_settings
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val appBarConfig = AppBarConfiguration(topLevelDestinations)
        setupActionBarWithNavController(navController, appBarConfig)
        setupTopNavigation(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp()
            || super.onSupportNavigateUp()
    }

    private fun setupTopNavigation(navController: NavController) {
        val chipDestinations = mapOf(
            binding.chipMyCollection to R.id.nav_my_collection,
            binding.chipAll to R.id.nav_all,
            binding.chipStorage to R.id.nav_storage,
            binding.chipWants to R.id.nav_wants,
            binding.chipDecks to R.id.nav_decks,
            binding.chipSettings to R.id.nav_settings
        )

        chipDestinations.forEach { (chip, destinationId) ->
            chip.setOnClickListener { navigateToTopLevel(navController, destinationId) }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val selectedChipId = chipDestinations.entries
                .firstOrNull { it.value == destination.id }
                ?.key
                ?.id

            if (selectedChipId != null) {
                binding.topNavChips.check(selectedChipId)
            } else {
                binding.topNavChips.clearCheck()
            }
        }
    }

    private fun navigateToTopLevel(navController: NavController, destinationId: Int) {
        if (navController.currentDestination?.id == destinationId) return

        val options = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
        }

        navController.navigate(destinationId, null, options)
    }
}
