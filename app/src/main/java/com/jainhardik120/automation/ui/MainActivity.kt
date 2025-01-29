package com.jainhardik120.automation.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jainhardik120.automation.ui.home.LedControlScreen
import com.jainhardik120.automation.ui.profile_edit.MacropadProfileEditViewModel
import com.jainhardik120.automation.ui.profile_edit.ProfileEditScreen
import com.jainhardik120.automation.ui.theme.AutomationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ),
                0
            )
        }
        enableEdgeToEdge()
        setContent {
            AutomationTheme {
                val navController = rememberNavController()
                val viewModel: ApplicationViewModel = hiltViewModel()
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.MacropadProfilesScreen
                ) {
                    composable<AppRoutes.MacropadProfilesScreen> {
                        ProfileListScreen(
                            viewModel = viewModel,
                            onProfileClick = {
                                navController.navigate(AppRoutes.MacroPadProfileEditScreen(it))
                            })
                    }
                    composable<AppRoutes.MacroPadProfileEditScreen> {
                        val macropadProfileEditViewModel =
                            hiltViewModel<MacropadProfileEditViewModel>()
                        ProfileEditScreen(viewModel = macropadProfileEditViewModel)
                    }
                    composable<AppRoutes.LedControl> {
                        LedControlScreen(viewModel)
                    }
                }
            }
        }
    }
}


