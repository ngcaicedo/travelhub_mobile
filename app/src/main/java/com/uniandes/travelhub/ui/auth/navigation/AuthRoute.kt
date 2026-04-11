package com.uniandes.travelhub.ui.auth.navigation

/**
 * Routes used by [AuthNavGraph]. Kept as a small sealed class so the
 * destinations are exhaustive and discoverable from one place.
 */
sealed class AuthRoute(val route: String) {
    data object Login : AuthRoute("login")
    data object Register : AuthRoute("register")

    /**
     * VerifyOtp accepts the user's email as a path argument so the screen can
     * display it in the subtitle and the ViewModel can submit the OTP for that
     * specific account.
     */
    data object VerifyOtp : AuthRoute("verify_otp/{email}") {
        const val ARG_EMAIL = "email"
        fun build(email: String): String = "verify_otp/$email"
    }

    /** Final destinations based on the user's role. */
    data object TravelerHome : AuthRoute("traveler_home")
    data object PartnerHome : AuthRoute("partner_home")
    data object AdminHome : AuthRoute("admin_home")
}
