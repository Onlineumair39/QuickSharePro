package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.ShareViewModel
import com.example.ui.ShareViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Retrieve repositories from custom Application class
            val app = application as QuickShareApp
            val transferRepo = app.transferRepository
            val settingsRepo = app.settingsRepository

            // Construct ViewModel using our customized factory
            val shareViewModel: ShareViewModel = viewModel(
                factory = ShareViewModelFactory(
                    application = app,
                    transferRepository = transferRepo,
                    settingsRepository = settingsRepo
                )
            )

            // Monitor state flows
            val darkThemeSetting by shareViewModel.darkThemeMode.collectAsStateWithLifecycle()

            // Resolve dynamic styling system based on mode selections (index 0=System, 1=Light, 2=Dark)
            val useDarkTheme = when (darkThemeSetting) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
                                viewModel = shareViewModel,
                                onNavigateToSend = { navController.navigate("send") },
                                onNavigateToReceive = { navController.navigate("receive") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToSettings = { navController.navigate("settings") },
                                onNavigateToPrivacy = { navController.navigate("privacy") },
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToTerms = { navController.navigate("terms") },
                                onNavigateToContact = { navController.navigate("contact") }
                            )
                        }

                        composable("send") {
                            SendScreen(
                                viewModel = shareViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("receive") {
                            ReceiveScreen(
                                viewModel = shareViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("history") {
                            HistoryScreen(
                                viewModel = shareViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("settings") {
                            SettingsScreen(
                                viewModel = shareViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPrivacy = { navController.navigate("privacy") },
                                onNavigateToAbout = { navController.navigate("about") },
                                onNavigateToContact = { navController.navigate("contact") }
                            )
                        }

                        composable("privacy") {
                            PrivacyPolicyScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("about") {
                            AboutUsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("terms") {
                            TermsConditionsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable("contact") {
                            ContactUsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
