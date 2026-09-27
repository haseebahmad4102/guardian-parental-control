package com.example.guardian.parent.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.guardian.parent.ui.auth.AuthScreen
import com.example.guardian.parent.ui.auth.AuthViewModel
import com.example.guardian.parent.ui.device.DeviceScreen
import com.example.guardian.parent.ui.home.HomeScreen
import com.example.guardian.parent.ui.pair.PairScreen
import com.example.guardian.parent.ui.policy.DevicePolicyScreen
import com.example.guardian.parent.ui.provisioning.ProvisioningScreen
import com.example.guardian.parent.ui.screen.ScreenViewScreen

private object Routes {
    const val HOME = "home"
    const val PAIR = "pair"
    const val DEVICE = "device"
    const val POLICY = "policy"
    const val SCREEN = "screen"
    const val PROVISIONING = "provisioning"
    const val ARG_DEVICE_ID = "deviceId"
}

/**
 * Top-level gate. Signed-out shows [AuthScreen]; signed-in shows a small nav graph
 * (dashboard ⇄ pairing). The Activity-scoped [AuthViewModel] drives the sign-in/out flip.
 */
@Composable
fun GuardianRoot() {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isSignedIn by authViewModel.isSignedIn.collectAsStateWithLifecycle()

    when (isSignedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        false -> AuthScreen(authViewModel)
        true -> SignedInGraph(onSignOut = authViewModel::signOut)
    }
}

@Composable
private fun SignedInGraph(onSignOut: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onPair = { navController.navigate(Routes.PAIR) },
                onSignOut = onSignOut,
                onOpenDevice = { device ->
                    navController.navigate("${Routes.DEVICE}/${device.deviceId}")
                },
                onSetupDeviceOwner = { navController.navigate(Routes.PROVISIONING) },
            )
        }
        composable(Routes.PROVISIONING) {
            ProvisioningScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PAIR) {
            PairScreen(onDone = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.DEVICE}/{${Routes.ARG_DEVICE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString(Routes.ARG_DEVICE_ID).orEmpty()
            DeviceScreen(
                onBack = { navController.popBackStack() },
                onOpenPolicy = { navController.navigate("${Routes.POLICY}/$deviceId") },
                onViewScreen = { navController.navigate("${Routes.SCREEN}/$deviceId") },
            )
        }
        composable(
            route = "${Routes.POLICY}/{${Routes.ARG_DEVICE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) {
            DevicePolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = "${Routes.SCREEN}/{${Routes.ARG_DEVICE_ID}}",
            arguments = listOf(navArgument(Routes.ARG_DEVICE_ID) { type = NavType.StringType }),
        ) {
            ScreenViewScreen(onBack = { navController.popBackStack() })
        }
    }
}
