import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kover)
}

// Aplicar el plugin de google-services solo si existe el archivo de configuración.
// Esto permite compilar y correr unit tests en CI / dev sin Firebase configurado.
// Para que las push notifications funcionen en runtime hay que descargar
// google-services.json del Firebase Console (ver SETUP_HU020.md).
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

// Read backend base URLs from local.properties (with safe defaults for the Android emulator).
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}
val securityApiBase: String = localProperties.getProperty(
    "TRAVELHUB_SECURITY_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val usersApiBase: String = localProperties.getProperty(
    "TRAVELHUB_USERS_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val propertiesApiBase: String = localProperties.getProperty(
    "TRAVELHUB_PROPERTIES_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val searchApiBase: String = localProperties.getProperty(
    "TRAVELHUB_SEARCH_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val reservationsApiBase: String = localProperties.getProperty(
    "TRAVELHUB_RESERVATIONS_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val paymentsApiBase: String = localProperties.getProperty(
    "TRAVELHUB_PAYMENTS_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val notificationsApiBase: String = localProperties.getProperty(
    "TRAVELHUB_NOTIFICATIONS_API_BASE",
    "http://travelhub-dev-alb-932523405.us-east-1.elb.amazonaws.com/"
)
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

android {
    namespace = "com.uniandes.travelhub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.uniandes.travelhub"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SECURITY_API_BASE", "\"$securityApiBase\"")
        buildConfigField("String", "USERS_API_BASE", "\"$usersApiBase\"")
        buildConfigField("String", "PROPERTIES_API_BASE", "\"$propertiesApiBase\"")
        buildConfigField("String", "SEARCH_API_BASE", "\"$searchApiBase\"")
        buildConfigField("String", "RESERVATIONS_API_BASE", "\"$reservationsApiBase\"")
        buildConfigField("String", "PAYMENTS_API_BASE", "\"$paymentsApiBase\"")
        buildConfigField("String", "NOTIFICATIONS_API_BASE", "\"$notificationsApiBase\"")

        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = false
                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
                }
            }
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes(
                    // Moshi-generated JSON adapters (KSP)
                    "*JsonAdapter",
                    "*JsonAdapter\$*",
                    // Compose compiler-generated singletons
                    "*.ComposableSingletons\$*",
                    // BuildConfig
                    "*.BuildConfig",
                    // MainActivity (Android entry point)
                    "*.MainActivity",
                    "*.MainActivity\$*",
                    // Compose UI screens & components (not unit-testable)
                    "*.ui.auth.navigation.*",
                    "*.ui.auth.login.LoginScreenKt*",
                    "*.ui.auth.register.RegisterScreenKt*",
                    "*.ui.auth.verifyotp.VerifyOtpScreenKt*",
                    "*.ui.auth.home.*",
                    "*.ui.auth.components.PasswordStrengthMeterKt*",
                    "*.ui.auth.components.ErrorMessageResolverKt*",
                    "*.ui.properties.*",
                )
            }
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi)
    ksp(libs.moshi.kotlin.codegen)

    implementation(libs.coil.compose)
    implementation(libs.stripe.android)

    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}