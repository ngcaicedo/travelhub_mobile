package com.uniandes.travelhub.ui.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.ui.auth.home.PlaceholderHomeScreen
import com.uniandes.travelhub.ui.auth.login.LoginScreen
import com.uniandes.travelhub.ui.auth.register.RegisterScreen
import com.uniandes.travelhub.ui.auth.verifyotp.VerifyOtpScreen
import com.uniandes.travelhub.ui.properties.PropertyDetailScreen
import com.uniandes.travelhub.ui.properties.PropertyListScreen
import com.uniandes.travelhub.viewmodels.LoginViewModel
import com.uniandes.travelhub.viewmodels.PropertiesViewModel
import com.uniandes.travelhub.viewmodels.PropertyDetailViewModel
import com.uniandes.travelhub.viewmodels.RegisterViewModel
import com.uniandes.travelhub.viewmodels.VerifyOtpViewModel
import kotlinx.coroutines.launch

/**
 * Wires every auth destination together. Owns the [NavHostController] and
 * forwards locale state to the screens that render the [LanguageSwitcher].
 */
@Composable
fun AuthNavGraph(
    authRepository: AuthRepository,
    propertiesRepository: PropertiesRepository,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val userRole by authRepository.observeRole().collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login.route,
    ) {
        composable(AuthRoute.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository)
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
                factory = RegisterViewModel.Factory(authRepository)
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
                factory = VerifyOtpViewModel.Factory(authRepository, email)
            )
            VerifyOtpScreen(
                viewModel = viewModel,
                email = email,
                onNavigateToHome = { role ->
                    val destination = when (role) {
                        UserRole.TRAVELER -> AuthRoute.TravelerHome.route
                        UserRole.HOTEL_PARTNER -> AuthRoute.PartnerHome.route
                        UserRole.ADMIN -> AuthRoute.AdminHome.route
                    }
                    navController.navigate(destination) {
                        popUpTo(AuthRoute.Login.route) { inclusive = true }
                    }
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
            val viewModel: PropertiesViewModel = viewModel(
                factory = PropertiesViewModel.Factory(propertiesRepository)
            )
            PropertyListScreen(
                viewModel = viewModel,
                onPropertyClick = { id ->
                    navController.navigate(AuthRoute.PropertyDetail.build(id))
                },
                onLoggedOut = {
                    scope.launch {
                        authRepository.logout()
                        navController.navigate(AuthRoute.Login.route) {
                            popUpTo(AuthRoute.TravelerHome.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(AuthRoute.PartnerHome.route) {
            PlaceholderHomeScreen(
                repository = authRepository,
                titleRes = R.string.home_partner_dashboard_title,
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(AuthRoute.PlaceholderHome.route) {
            val propertiesViewModel: PropertiesViewModel = viewModel(
                factory = PropertiesViewModel.Factory(propertiesRepository)
            )
            PropertyListScreen(
                viewModel = propertiesViewModel,
                onPropertyClick = { propertyId ->
                    navController.navigate(AuthRoute.PropertyDetail.build(propertyId))
                },
                onLoggedOut = {
                    scope.launch {
                        authRepository.logout()
                        navController.navigate(AuthRoute.Login.route) {
                            popUpTo(AuthRoute.PlaceholderHome.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(
            route = AuthRoute.PropertyDetail.route,
            arguments = listOf(navArgument(AuthRoute.PropertyDetail.ARG_ID) {
                type = NavType.StringType
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(AuthRoute.PropertyDetail.ARG_ID).orEmpty()
            val detailViewModel: PropertyDetailViewModel = viewModel(
                factory = PropertyDetailViewModel.Factory(propertiesRepository, id)
            )
            PropertyDetailScreen(
                viewModel = detailViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(AuthRoute.AdminHome.route) {
            PlaceholderHomeScreen(
                repository = authRepository,
                titleRes = R.string.home_admin_dashboard_title,
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(AuthRoute.PropertyList.route) {
            val viewModel: PropertiesViewModel = viewModel(
                factory = PropertiesViewModel.Factory(propertiesRepository)
            )
            PropertyListScreen(
                viewModel = viewModel,
                onPropertyClick = { id ->
                    navController.navigate(AuthRoute.PropertyDetail.build(id))
                },
                onLoggedOut = {
                    scope.launch {
                        authRepository.logout()
                        navController.navigate(AuthRoute.Login.route) {
                            popUpTo(AuthRoute.PropertyList.route) { inclusive = true }
                        }
                    }
                }
            )
        }
    }
}
