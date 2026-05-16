package com.uniandes.travelhub.ui.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.uniandes.travelhub.R
import com.uniandes.travelhub.models.UserRole
import com.uniandes.travelhub.models.payments.PaymentConfirmationSummary
import com.uniandes.travelhub.models.properties.Property
import com.uniandes.travelhub.network.AuthTokenStore
import com.uniandes.travelhub.network.RetrofitFactory
import com.uniandes.travelhub.repositories.AuthRepository
import com.uniandes.travelhub.repositories.NotificationsRepository
import com.uniandes.travelhub.repositories.PaymentsRepository
import com.uniandes.travelhub.repositories.PropertiesRepository
import com.uniandes.travelhub.repositories.ReservationsRepository
import com.uniandes.travelhub.repositories.SearchRepository
import com.uniandes.travelhub.repositories.SeasonalPricingRepository
import com.uniandes.travelhub.ui.notifications.NotificationsListScreen
import com.uniandes.travelhub.ui.partner.pricing.EditRuleScreen
import com.uniandes.travelhub.ui.partner.pricing.PartnerPricingScreen
import com.uniandes.travelhub.ui.partner.pricing.RulesListScreen
import com.uniandes.travelhub.ui.profile.NotificationSettingsScreen
import com.uniandes.travelhub.ui.profile.ProfileScreen
import com.uniandes.travelhub.viewmodels.NotificationPreferencesViewModel
import com.uniandes.travelhub.viewmodels.NotificationsViewModel
import com.uniandes.travelhub.ui.auth.components.RequireRole
import com.uniandes.travelhub.ui.auth.home.PlaceholderHomeScreen
import com.uniandes.travelhub.ui.auth.login.LoginScreen
import com.uniandes.travelhub.ui.auth.register.RegisterScreen
import com.uniandes.travelhub.ui.auth.verifyotp.VerifyOtpScreen
import com.uniandes.travelhub.ui.checkout.CheckoutScreen
import com.uniandes.travelhub.ui.payment.PaymentConfirmationScreen
import com.uniandes.travelhub.ui.payment.PaymentScreen
import com.uniandes.travelhub.ui.properties.PropertyDetailScreen
import com.uniandes.travelhub.ui.reservations.ReservationDetailScreen
import com.uniandes.travelhub.ui.reservations.ReservationsListScreen
import com.uniandes.travelhub.ui.search.SearchScreen
import com.uniandes.travelhub.viewmodels.CheckoutViewModel
import com.uniandes.travelhub.viewmodels.LoginViewModel
import com.uniandes.travelhub.viewmodels.PaymentViewModel
import com.uniandes.travelhub.viewmodels.PropertyDetailViewModel
import com.uniandes.travelhub.viewmodels.RegisterViewModel
import com.uniandes.travelhub.viewmodels.ReservationDetailViewModel
import com.uniandes.travelhub.viewmodels.ReservationsListViewModel
import com.uniandes.travelhub.viewmodels.EditRuleViewModel
import com.uniandes.travelhub.viewmodels.PartnerPricingViewModel
import com.uniandes.travelhub.viewmodels.RulesListViewModel
import com.uniandes.travelhub.network.location.CityGeocoder
import com.uniandes.travelhub.network.location.LocationProvider
import com.uniandes.travelhub.viewmodels.MapSearchViewModel
import com.uniandes.travelhub.viewmodels.SearchViewModel
import com.uniandes.travelhub.viewmodels.VerifyOtpViewModel
import kotlinx.coroutines.launch

/**
 * Wires every destination together. Owns the [NavHostController] and the
 * [paymentConfirmationCache] used to pass the in-memory confirmation summary
 * from PaymentScreen → PaymentConfirmationScreen without serialising it.
 */
@Composable
fun AuthNavGraph(
    authRepository: AuthRepository,
    propertiesRepository: PropertiesRepository,
    searchRepository: SearchRepository,
    reservationsRepository: ReservationsRepository,
    paymentsRepository: PaymentsRepository,
    seasonalPricingRepository: SeasonalPricingRepository,
    tokenStore: AuthTokenStore,
    locationProvider: LocationProvider,
    cityGeocoder: CityGeocoder,
    currentLocale: String,
    onLocaleChange: (String) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val userRole by authRepository.observeRole().collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val paymentConfirmationCache = remember { mutableStateOf<PaymentConfirmationSummary?>(null) }
    val bookingCoverUrlCache = remember { mutableStateOf<String?>(null) }
    val notificationsRepository = remember {
        NotificationsRepository(RetrofitFactory.notificationsApi)
    }

    val onUnauthorized: () -> Unit = {
        navController.navigate(AuthRoute.Login.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Login.route,
    ) {
        composable(AuthRoute.Login.route) {
            val viewModel: LoginViewModel = viewModel(
                factory = LoginViewModel.Factory(authRepository)
            )
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
            if (userRole == null) {
                LoginScreen(
                    viewModel = viewModel,
                    onNavigateToOtp = { email -> navController.navigate(AuthRoute.VerifyOtp.build(email)) },
                    onNavigateToRegister = { navController.navigate(AuthRoute.Register.route) },
                    currentLocale = currentLocale,
                    onLocaleChange = onLocaleChange,
                )
            }
        }

        composable(AuthRoute.Register.route) {
            val viewModel: RegisterViewModel = viewModel(factory = RegisterViewModel.Factory(authRepository))
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack(AuthRoute.Login.route, inclusive = false) },
                currentLocale = currentLocale,
                onLocaleChange = onLocaleChange,
            )
        }

        composable(
            route = AuthRoute.VerifyOtp.route,
            arguments = listOf(navArgument(AuthRoute.VerifyOtp.ARG_EMAIL) { type = NavType.StringType }),
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString(AuthRoute.VerifyOtp.ARG_EMAIL).orEmpty()
            val viewModel: VerifyOtpViewModel = viewModel(factory = VerifyOtpViewModel.Factory(authRepository, email))
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
                onNavigateBackToLogin = { navController.popBackStack(AuthRoute.Login.route, inclusive = false) },
                currentLocale = currentLocale,
                onLocaleChange = onLocaleChange,
            )
        }

        composable(AuthRoute.TravelerHome.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: com.uniandes.travelhub.viewmodels.PropertiesViewModel = viewModel(
                    factory = com.uniandes.travelhub.viewmodels.PropertiesViewModel.Factory(propertiesRepository)
                )
                com.uniandes.travelhub.ui.properties.PropertyListScreen(
                    viewModel = viewModel,
                    onPropertyClick = { property: Property ->
                        propertiesRepository.primePropertyPreview(property)
                        navController.navigate(AuthRoute.PropertyDetail.build(property.id))
                    },
                    onSearchClick = { navController.navigate(AuthRoute.Search.route) },
                    onMyReservationsClick = { navController.navigate(AuthRoute.ReservationsList.route) },
                    onNotificationsClick = { navController.navigate(AuthRoute.Notifications.route) },
                    onProfileClick = { navController.navigate(AuthRoute.Profile.route) },
                    onLoggedOut = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate(AuthRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }

        composable(AuthRoute.Search.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory(searchRepository))
                val mapViewModel: MapSearchViewModel = viewModel(
                    factory = MapSearchViewModel.Factory(searchRepository, locationProvider, cityGeocoder)
                )
                SearchScreen(
                    viewModel = viewModel,
                    mapViewModel = mapViewModel,
                    onResultClick = { item ->
                        navController.navigate(AuthRoute.PropertyDetail.build(item.id))
                    },
                    onLoggedOut = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate(AuthRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onMyReservationsClick = {
                        navController.navigate(AuthRoute.ReservationsList.route)
                    },
                    onBackClick = {
                        if (!navController.popBackStack()) {
                            navController.navigate(AuthRoute.TravelerHome.route) {
                                popUpTo(AuthRoute.Search.route) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }

        composable(
            route = AuthRoute.PropertyDetail.route,
            arguments = listOf(navArgument(AuthRoute.PropertyDetail.ARG_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(AuthRoute.PropertyDetail.ARG_ID).orEmpty()
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val detailViewModel: PropertyDetailViewModel = viewModel(
                    factory = PropertyDetailViewModel.Factory(propertiesRepository, id)
                )
                PropertyDetailScreen(
                    viewModel = detailViewModel,
                    onBackClick = { navController.popBackStack() },
                    onReserveClick = { property: Property ->
                        navController.navigate(AuthRoute.Checkout.build(property.id))
                    },
                )
            }
        }

        composable(
            route = AuthRoute.Checkout.route,
            arguments = listOf(navArgument(AuthRoute.Checkout.ARG_PROPERTY_ID) { type = NavType.StringType }),
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments?.getString(AuthRoute.Checkout.ARG_PROPERTY_ID).orEmpty()
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: CheckoutViewModel = viewModel(
                    factory = CheckoutViewModel.Factory(
                        propertyId = propertyId,
                        reservationsRepository = reservationsRepository,
                        propertiesRepository = propertiesRepository,
                    )
                )
                CheckoutScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToPayment = { reservation ->
                        bookingCoverUrlCache.value = viewModel.property.value
                            ?.images
                            ?.sortedBy { it.position }
                            ?.firstOrNull { it.url.isNotBlank() }
                            ?.url
                        val cents = (reservation.priceBreakdown?.totalInCents
                            ?: parseCents(reservation.totalPrice))
                        navController.navigate(
                            AuthRoute.Payment.build(reservation.id, cents, reservation.currency)
                        )
                    },
                )
            }
        }

        composable(
            route = AuthRoute.Payment.route,
            arguments = listOf(
                navArgument(AuthRoute.Payment.ARG_RESERVATION_ID) { type = NavType.StringType },
                navArgument(AuthRoute.Payment.ARG_AMOUNT_IN_CENTS) { type = NavType.LongType },
                navArgument(AuthRoute.Payment.ARG_CURRENCY) { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            val reservationId = args?.getString(AuthRoute.Payment.ARG_RESERVATION_ID).orEmpty()
            val amountInCents = args?.getLong(AuthRoute.Payment.ARG_AMOUNT_IN_CENTS) ?: 0L
            val currency = args?.getString(AuthRoute.Payment.ARG_CURRENCY).orEmpty()
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: PaymentViewModel = viewModel(
                    factory = PaymentViewModel.Factory(reservationId, amountInCents, currency, paymentsRepository)
                )
                PaymentScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToConfirmation = { confirmation ->
                        paymentConfirmationCache.value = confirmation
                        navController.navigate(AuthRoute.PaymentConfirmation.build(confirmation.paymentId)) {
                            popUpTo(AuthRoute.Search.route) { inclusive = false }
                        }
                    },
                )
            }
        }

        composable(
            route = AuthRoute.PaymentConfirmation.route,
            arguments = listOf(navArgument(AuthRoute.PaymentConfirmation.ARG_PAYMENT_ID) { type = NavType.StringType }),
        ) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val confirmation = paymentConfirmationCache.value
                if (confirmation == null) {
                    // Cache lost (e.g. process death) — bounce to the reservations list.
                    LaunchedEffect(Unit) {
                        navController.navigate(AuthRoute.ReservationsList.route) {
                            popUpTo(AuthRoute.Search.route) { inclusive = false }
                        }
                    }
                } else {
                    PaymentConfirmationScreen(
                        confirmation = confirmation,
                        propertyCoverUrl = bookingCoverUrlCache.value,
                        onSeeReservationsClick = {
                            navController.navigate(AuthRoute.ReservationsList.route) {
                                popUpTo(AuthRoute.Search.route) { inclusive = false }
                            }
                        },
                    )
                }
            }
        }

        composable(AuthRoute.ReservationsList.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: ReservationsListViewModel = viewModel(
                    factory = ReservationsListViewModel.Factory(reservationsRepository)
                )
                ReservationsListScreen(
                    viewModel = viewModel,
                    onReservationClick = { reservation ->
                        navController.navigate(AuthRoute.ReservationDetail.build(reservation.id))
                    },
                    onBackClick = { navController.popBackStack() },
                    onSearchClick = {
                        navController.navigate(AuthRoute.Search.route) {
                            popUpTo(AuthRoute.Search.route) { inclusive = false }
                        }
                    },
                )
            }
        }

        composable(
            route = AuthRoute.ReservationDetail.route,
            arguments = listOf(navArgument(AuthRoute.ReservationDetail.ARG_ID) { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://travelhub.app/reservations/{id}" },
                navDeepLink { uriPattern = "travelhub://reservation/{id}" },
            ),
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString(AuthRoute.ReservationDetail.ARG_ID).orEmpty()
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: ReservationDetailViewModel = viewModel(
                    factory = ReservationDetailViewModel.Factory(id, reservationsRepository)
                )
                ReservationDetailScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                )
            }
        }

        composable(AuthRoute.Notifications.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: NotificationsViewModel = viewModel(
                    factory = NotificationsViewModel.Factory(notificationsRepository)
                )
                NotificationsListScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(AuthRoute.NotificationSettings.route) },
                    onItemClick = { item ->
                        if (item.entity_type == "reservation") {
                            navController.navigate(AuthRoute.ReservationDetail.build(item.entity_id))
                        }
                    },
                )
            }
        }

        composable(AuthRoute.Profile.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                ProfileScreen(
                    displayName = "Viajero",
                    email = "",
                    onBackClick = { navController.popBackStack() },
                    onNotificationsClick = { navController.navigate(AuthRoute.NotificationSettings.route) },
                    onLogout = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate(AuthRoute.Login.route) { popUpTo(0) { inclusive = true } }
                        }
                    },
                )
            }
        }

        composable(AuthRoute.NotificationSettings.route) {
            RequireRole(tokenStore = tokenStore, requiredRole = UserRole.TRAVELER, onUnauthorized = onUnauthorized) {
                val viewModel: NotificationPreferencesViewModel = viewModel(
                    factory = NotificationPreferencesViewModel.Factory(notificationsRepository)
                )
                NotificationSettingsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                )
            }
        }

        composable(AuthRoute.PartnerHome.route) {
            // Partner landing redirects straight into pricing management (MPF-40):
            // no dashboard / KPIs are in scope.
            LaunchedEffect(Unit) {
                navController.navigate(AuthRoute.PartnerPricing.route) {
                    popUpTo(AuthRoute.PartnerHome.route) { inclusive = true }
                }
            }
        }

        composable(AuthRoute.PartnerPricing.route) {
            RequireRole(
                tokenStore = tokenStore,
                requiredRole = UserRole.HOTEL_PARTNER,
                onUnauthorized = onUnauthorized,
            ) {
                val pricingViewModel: PartnerPricingViewModel = viewModel(
                    factory = PartnerPricingViewModel.Factory(
                        propertiesRepository = propertiesRepository,
                        seasonalPricingRepository = seasonalPricingRepository,
                        tokenStore = tokenStore,
                    )
                )
                PartnerPricingScreen(
                    viewModel = pricingViewModel,
                    onRulesTabClick = {
                        navController.navigate(AuthRoute.PartnerPricingRules.route) {
                            popUpTo(AuthRoute.PartnerPricing.route) { inclusive = false }
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                    onLogout = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate(AuthRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                )
            }
        }

        composable(AuthRoute.PartnerPricingRules.route) {
            RequireRole(
                tokenStore = tokenStore,
                requiredRole = UserRole.HOTEL_PARTNER,
                onUnauthorized = onUnauthorized,
            ) {
                val rulesViewModel: RulesListViewModel = viewModel(
                    factory = RulesListViewModel.Factory(
                        propertiesRepository = propertiesRepository,
                        seasonalPricingRepository = seasonalPricingRepository,
                        tokenStore = tokenStore,
                    )
                )
                RulesListScreen(
                    viewModel = rulesViewModel,
                    onPricingTabClick = {
                        navController.navigate(AuthRoute.PartnerPricing.route) {
                            popUpTo(AuthRoute.PartnerPricing.route) { inclusive = false }
                        }
                    },
                    onRuleClick = { propertyId, ruleId ->
                        navController.navigate(
                            AuthRoute.PartnerPricingEditRule.build(propertyId, ruleId)
                        )
                    },
                    onBackClick = { navController.popBackStack() },
                )
            }
        }

        composable(
            route = AuthRoute.PartnerPricingEditRule.route,
            arguments = listOf(
                navArgument(AuthRoute.PartnerPricingEditRule.ARG_PROPERTY_ID) {
                    type = NavType.StringType
                },
                navArgument(AuthRoute.PartnerPricingEditRule.ARG_RULE_ID) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val propertyId = backStackEntry.arguments
                ?.getString(AuthRoute.PartnerPricingEditRule.ARG_PROPERTY_ID).orEmpty()
            val ruleId = backStackEntry.arguments
                ?.getString(AuthRoute.PartnerPricingEditRule.ARG_RULE_ID).orEmpty()
            RequireRole(
                tokenStore = tokenStore,
                requiredRole = UserRole.HOTEL_PARTNER,
                onUnauthorized = onUnauthorized,
            ) {
                val editViewModel: EditRuleViewModel = viewModel(
                    factory = EditRuleViewModel.Factory(
                        propertiesRepository = propertiesRepository,
                        seasonalPricingRepository = seasonalPricingRepository,
                        propertyId = propertyId,
                        ruleId = ruleId,
                    )
                )
                EditRuleScreen(
                    viewModel = editViewModel,
                    onBackClick = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
        }

        composable(AuthRoute.AdminHome.route) {
            PlaceholderHomeScreen(
                repository = authRepository,
                titleRes = R.string.home_admin_dashboard_title,
                onLoggedOut = {
                    navController.navigate(AuthRoute.Login.route) { popUpTo(0) { inclusive = true } }
                },
            )
        }
    }
}

private fun parseCents(totalPrice: String): Long =
    runCatching { (totalPrice.toDouble() * 100).toLong() }.getOrDefault(0L)
