package com.uniandes.travelhub.ui.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.ui.auth.home.PlaceholderHomeScreen
import com.uniandes.travelhub.ui.auth.login.LoginScreen
import com.uniandes.travelhub.ui.auth.register.RegisterScreen
import com.uniandes.travelhub.ui.auth.verifyotp.VerifyOtpScreen
import com.uniandes.travelhub.viewmodels.LoginViewModel
import com.uniandes.travelhub.viewmodels.RegisterViewModel
import com.uniandes.travelhub.viewmodels.VerifyOtpViewModel

/**
 * Wires every auth destination together. Owns the [NavHostController] and
 * forwards locale state to the screens that render the [LanguageSwitcher].
 */
@Composable
fun AuthNavGraph(
    repository: AuthRepository,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val userRole by repository.observeRole().collectAsState(initial = null)

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login.route,
    ) {
        composable(AuthRoute.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(repository)
            )
            
            // If the user is already logged in, navigate to their home based on role
            LaunchedEffect(userRole) {
                userRole?.let { role ->
                    val destination = when (role) {
                        UserRole.TRAVELER -> AuthRoute.TravelerHome.route
                        UserRole.HOTEL_PARTNER -> AuthRoute.PartnerHome.route
                        UserRole.ADMIN -> AuthRoute.AdminHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(AuthRoute.Login.route) { inclusive = true }
                    }
                }
            }

            LoginScreen(
                viewModel = viewModel,
                onNavigateToOtp = { email ->
                    navController.navigate(AuthRoute.VerifyOtp.build(email))
                },
                onNavigateToRegister = {
                    navController.navigate(AuthRoute.Register.route)
                },
                currentLocale = currentLocale,
                onLocaleChange = onLocaleChange,
            )
        }

        composable(AuthRoute.Register.route) {
            val viewModel: RegisterViewModel = viewModel(
                factory = RegisterViewModel.Factory(repository)
            )
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.popBackStack(
                        route = AuthRoute.Login.route,
                        inclusive = false,
                    )
                },
                currentLocale = currentLocale,
                onLocaleChange = onLocaleChange,
            )
        }

        composable(
            route = AuthRoute.VerifyOtp.route,
            arguments = listOf(navArgument(AuthRoute.VerifyOtp.ARG_EMAIL) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString(AuthRoute.VerifyOtp.ARG_EMAIL).orEmpty()
            val viewModel: VerifyOtpViewModel = viewModel(
                factory = VerifyOtpViewModel.Factory(repository, email)
            )
            VerifyOtpScreen(
                viewModel = viewModel,
                email = email,
                onNavigateToHome = {
                    // Handled by role observation in the next screen or globally
                },
                onNavigateBackToLogin = {
                    navController.popBackStack(
                        route = AuthRoute.Login.route,
                        inclusive = false,
                    )
                },
                currentLocale = currentLocale,
                onLocaleChange = onLocaleChange,
            )
        }

        composable(AuthRoute.TravelerHome.route) {
            PlaceholderHomeScreen(
                repository = repository,
                title = "Traveler Dashboard",
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(AuthRoute.PartnerHome.route) {
            PlaceholderHomeScreen(
                repository = repository,
                title = "Hotel Partner Dashboard",
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(AuthRoute.AdminHome.route) {
            PlaceholderHomeScreen(
                repository = repository,
                title = "Admin Dashboard",
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
