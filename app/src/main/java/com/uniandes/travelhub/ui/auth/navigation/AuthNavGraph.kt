package com.uniandes.travelhub.ui.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.repositories.PropertiesRepository
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
    repository: AuthRepository,
    propertiesRepository: PropertiesRepository,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login.route,
    ) {
        composable(AuthRoute.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(repository)
            )
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
                    navController.navigate(AuthRoute.PlaceholderHome.route) {
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

        composable(AuthRoute.PlaceholderHome.route) {
            val propertiesViewModel: PropertiesViewModel = viewModel(
                factory = PropertiesViewModel.Factory(propertiesRepository)
            )
            val scope = rememberCoroutineScope()
            PropertyListScreen(
                viewModel = propertiesViewModel,
                onPropertyClick = { propertyId ->
                    navController.navigate(AuthRoute.PropertyDetail.build(propertyId))
                },
                onLoggedOut = {
                    scope.launch {
                        repository.logout()
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
    }
}
