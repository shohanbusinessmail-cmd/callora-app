package com.callora.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.callora.app.core.designsystem.theme.CalloraTheme
import com.callora.app.feature.about.AboutScreen
import com.callora.app.feature.dashboard.DashboardScreen
import com.callora.app.feature.dashboard.DashboardViewModel
import com.callora.app.feature.history.HistoryScreen
import com.callora.app.feature.history.HistoryViewModel
import com.callora.app.feature.onboarding.OnboardingScreen
import com.callora.app.feature.onboarding.OnboardingViewModel
import com.callora.app.feature.profiles.ProfileEditScreen
import com.callora.app.feature.profiles.ProfilesScreen
import com.callora.app.feature.profiles.ProfilesViewModel
import com.callora.app.feature.scheduling.SchedulingScreen
import com.callora.app.feature.scheduling.SchedulingViewModel
import com.callora.app.feature.settings.SettingsScreen
import com.callora.app.feature.settings.SettingsViewModel
import com.callora.app.feature.sims.SimDetailScreen
import com.callora.app.feature.sims.SimsScreen
import com.callora.app.feature.sims.SimsViewModel
import com.callora.app.feature.troubleshooting.TroubleshootingScreen
import com.callora.app.navigation.Routes
import com.callora.app.navigation.TopLevelDestination

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as CalloraApplication
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

            CalloraTheme(themeMode = settings.themeMode) {
                val navController = rememberNavController()
                val startDestination = if (settings.onboardingCompleted) Routes.DASHBOARD else Routes.ONBOARDING

                Scaffold(
                    bottomBar = {
                        val destination = navController.currentDestination
                        val showBar = TopLevelDestination.entries.any { it.route == destination?.route }
                        if (showBar) {
                            NavigationBar {
                                TopLevelDestination.entries.forEach { item ->
                                    val selected = destination?.hierarchy
                                        ?.any { it.route == item.route } == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = { Icon(item.icon, contentDescription = null) },
                                        label = { Text(stringResource(item.labelRes)) },
                                    )
                                }
                            }
                        }
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(Routes.ONBOARDING) {
                            val vm: OnboardingViewModel = viewModel(factory = OnboardingViewModel.factory(app))
                            OnboardingScreen(
                                viewModel = vm,
                                onFinished = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(Routes.DASHBOARD) {
                            val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(app))
                            DashboardScreen(
                                viewModel = vm,
                                onOpenSims = { navController.navigate(Routes.SIMS) },
                                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                            )
                        }
                        composable(Routes.SIMS) {
                            val vm: SimsViewModel = viewModel(factory = SimsViewModel.factory(app))
                            SimsScreen(
                                viewModel = vm,
                                onOpenSim = { subId -> navController.navigate(Routes.simDetail(subId)) },
                            )
                        }
                        composable(
                            route = Routes.SIM_DETAIL,
                            arguments = listOf(navArgument("subId") { type = NavType.IntType }),
                        ) { backStackEntry ->
                            val subId = backStackEntry.arguments?.getInt("subId") ?: return@composable
                            val vm: SimsViewModel = viewModel(factory = SimsViewModel.factory(app))
                            SimDetailScreen(
                                viewModel = vm,
                                subscriptionId = subId,
                                onBack = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.PROFILES) {
                            val vm: ProfilesViewModel = viewModel(factory = ProfilesViewModel.factory(app))
                            ProfilesScreen(
                                viewModel = vm,
                                onEdit = { id -> navController.navigate(Routes.profileEdit(id)) },
                                onOpenScheduling = { navController.navigate(Routes.SCHEDULING) },
                            )
                        }
                        composable(
                            route = Routes.PROFILE_EDIT,
                            arguments = listOf(navArgument("profileId") { type = NavType.LongType }),
                        ) { backStackEntry ->
                            val profileId = backStackEntry.arguments?.getLong("profileId") ?: -1L
                            val vm: ProfilesViewModel = viewModel(factory = ProfilesViewModel.factory(app))
                            ProfileEditScreen(
                                viewModel = vm,
                                profileId = profileId,
                                onSaved = { navController.popBackStack() },
                            )
                        }
                        composable(Routes.SCHEDULING) {
                            val vm: SchedulingViewModel = viewModel(factory = SchedulingViewModel.factory(app))
                            SchedulingScreen(viewModel = vm)
                        }
                        composable(Routes.HISTORY) {
                            val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(app))
                            HistoryScreen(viewModel = vm)
                        }
                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onAbout = { navController.navigate(Routes.ABOUT) },
                                onTroubleshooting = { navController.navigate(Routes.TROUBLESHOOTING) },
                                onReplayOnboarding = { navController.navigate(Routes.ONBOARDING) },
                            )
                        }
                        composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
                        composable(Routes.TROUBLESHOOTING) {
                            TroubleshootingScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
