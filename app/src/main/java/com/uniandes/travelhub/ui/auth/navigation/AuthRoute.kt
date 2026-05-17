package com.uniandes.travelhub.ui.auth.navigation

/**
 * Routes used by [AuthNavGraph]. Kept as a small sealed class so the
 * destinations are exhaustive and discoverable from one place.
 */
sealed class AuthRoute(val route: String) {
    data object Login : AuthRoute("login")
    data object Register : AuthRoute("register")

    data object VerifyOtp : AuthRoute("verify_otp/{email}") {
        const val ARG_EMAIL = "email"
        fun build(email: String): String = "verify_otp/$email"
    }

    /** Temporary destination shown after a successful OTP verification. */
    data object PlaceholderHome : AuthRoute("home")

    /** Final destinations based on the user's role. */
    data object TravelerHome : AuthRoute("traveler_home")
    data object PartnerHome : AuthRoute("partner_home")
    data object AdminHome : AuthRoute("admin_home")

    /** Property list and detail routes. */
    data object PropertyList : AuthRoute("property_list")
    data object PropertyDetail : AuthRoute("property_detail/{id}") {
        const val ARG_ID = "id"
        fun build(id: String): String = "property_detail/$id"
    }

    data object Search : AuthRoute("search")

    data object Checkout : AuthRoute("checkout/{propertyId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        fun build(propertyId: String): String = "checkout/$propertyId"
    }

    data object Payment : AuthRoute("payment/{reservationId}/{amountInCents}/{currency}") {
        const val ARG_RESERVATION_ID = "reservationId"
        const val ARG_AMOUNT_IN_CENTS = "amountInCents"
        const val ARG_CURRENCY = "currency"
        fun build(reservationId: String, amountInCents: Long, currency: String): String =
            "payment/$reservationId/$amountInCents/$currency"
    }

    data object PaymentConfirmation : AuthRoute("payment_confirmation/{paymentId}") {
        const val ARG_PAYMENT_ID = "paymentId"
        fun build(paymentId: String): String = "payment_confirmation/$paymentId"
    }

    data object ReservationsList : AuthRoute("reservations")
    data object ReservationDetail : AuthRoute("reservation/{id}") {
        const val ARG_ID = "id"
        fun build(id: String): String = "reservation/$id"
    }
    data object CheckInQr : AuthRoute("reservation/{id}/checkin_qr") {
        const val ARG_ID = "id"
        fun build(id: String): String = "reservation/$id/checkin_qr"
    }

    data object Notifications : AuthRoute("notifications")
    data object Profile : AuthRoute("profile")
    data object NotificationSettings : AuthRoute("profile/notifications")

    /** Partner: seasonal pricing management */
    data object PartnerPricing : AuthRoute("partner/pricing")
    data object PartnerPricingRules : AuthRoute("partner/pricing/rules")
    data object PartnerPricingEditRule : AuthRoute("partner/pricing/{propertyId}/rules/{ruleId}") {
        const val ARG_PROPERTY_ID = "propertyId"
        const val ARG_RULE_ID = "ruleId"
        fun build(propertyId: String, ruleId: String): String =
            "partner/pricing/$propertyId/rules/$ruleId"
    }
}