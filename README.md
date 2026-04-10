# TravelHub Mobile

App Android nativa de TravelHub. Cliente móvil del ecosistema de microservicios (Security + Users) que implementa el flujo de autenticación con OTP, registro multi-rol, y persistencia local de sesión.

**Stack:** Kotlin · Jetpack Compose (Material 3) · MVVM · Retrofit + Moshi · Coroutines/Flow · DataStore · Navigation Compose.

---

## Tabla de contenidos

1. [Empezando](#empezando)
2. [Cómo correr los tests](#cómo-correr-los-tests)
3. [Arquitectura](#arquitectura)
4. [Estructura del proyecto](#estructura-del-proyecto)
5. [Cómo extender el proyecto](#cómo-extender-el-proyecto)
6. [Convenciones](#convenciones)
7. [Build y debugging](#build-y-debugging)
8. [Checklist de PR](#checklist-de-pr)
9. [FAQ](#faq)

---

## Empezando

### Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 (compila a bytecode 11) |
| Android Studio | Ladybug o superior (AGP 8.13.x) |
| Android SDK | `compileSdk = 36`, `minSdk = 24` |
| Kotlin | 2.0.21 (gestionado por el wrapper) |

> No instales Gradle a mano: usa siempre `./gradlew`.

### 1. Clonar y configurar el entorno

```bash
git clone <repo>
cd travelhub-mobile
cp local.properties.example local.properties
```

### 2. Configurar las URLs de los microservicios

Edita `local.properties` con las bases de tu entorno (este archivo está en `.gitignore`):

```properties
TRAVELHUB_SECURITY_API_BASE=http://10.0.2.2:8001/
TRAVELHUB_USERS_API_BASE=http://10.0.2.2:8000/
```

⚠️ Las URLs **deben terminar en `/`** porque Retrofit las concatena con paths relativos. `10.0.2.2` es el alias del emulador para `localhost` del host.

Estos valores se exponen como `BuildConfig.SECURITY_API_BASE` y `BuildConfig.USERS_API_BASE` (ver `app/build.gradle.kts`).

### 3. Ejecutar la app

```bash
./gradlew installDebug   # instala el APK debug en el emulador/dispositivo conectado
```

O abre el proyecto en Android Studio y dale Run ▶.

---

## Cómo correr los tests

> El grueso del testing del proyecto vive en **unit tests**. Usamos Robolectric para ejecutar Compose en JVM sin necesidad de emulador — es **mucho** más rápido y CI-friendly.

### Todos los tests JVM

```bash
./gradlew testDebugUnitTest
```

Reporte HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

### Un test específico

Por clase:
```bash
./gradlew testDebugUnitTest --tests "com.uniandes.travelhub.viewmodels.LoginViewModelTest"
```

Por método:
```bash
./gradlew testDebugUnitTest --tests "com.uniandes.travelhub.viewmodels.LoginViewModelTest.submit_emits_navigation_event_on_success"
```

Por paquete (wildcard):
```bash
./gradlew testDebugUnitTest --tests "com.uniandes.travelhub.network.*"
```

Salida verbosa:
```bash
./gradlew testDebugUnitTest --info
```

### Tests de instrumentación (requieren emulador/dispositivo)

```bash
./gradlew connectedAndroidTest
```

Reporte: `app/build/reports/androidTests/connected/index.html`.

### Limpiar y re-ejecutar

```bash
./gradlew clean test
```

### Herramientas de testing disponibles

| Lib | Para qué se usa |
|---|---|
| **JUnit 4** | Runner base de los tests JVM |
| **kotlinx-coroutines-test** | `runTest`, control de tiempo virtual |
| **MockK** | Mocks idiomáticos de Kotlin (`mockk()`, `coEvery {}`, `coVerify {}`) |
| **Turbine** | Testear `Flow`/`StateFlow` con `flow.test { awaitItem() }` |
| **MockWebServer** | Servidor HTTP en memoria para tests de Retrofit |
| **Robolectric** | Ejecuta código Android (Context, recursos, Compose) en JVM |
| **Compose UI Test** | `createComposeRule()`, `onNodeWithText`, `performClick`, etc. |

### Plantillas / archivos de referencia

Cuando agregues tests nuevos, copia el patrón de:

| Tipo | Archivo de referencia |
|---|---|
| ViewModel | `app/src/test/java/com/uniandes/travelhub/viewmodels/LoginViewModelTest.kt` |
| Repository | `repositories/AuthRepositoryTest.kt` |
| Retrofit Api | `network/SecurityApiTest.kt`, `network/UsersApiTest.kt` |
| Parser de errores | `network/ApiErrorParserTest.kt` |
| Validadores / utils | `utils/AuthValidatorsTest.kt` |
| DataStore | `network/AuthTokenStoreTest.kt` |
| Compose UI pura | `ui/auth/login/LoginScreenContentTest.kt` |

> Los tests de ViewModel usan `MainDispatcherRule` (en `testing/MainDispatcherRule.kt`) para sustituir el `Main` dispatcher por uno de test.
> Los tests de Compose en JVM funcionan gracias a `unitTests { isIncludeAndroidResources = true }` en `app/build.gradle.kts`.

---

## Arquitectura

MVVM clásico con flujo unidireccional:

```
Compose UI ── observa StateFlow ──▶ ViewModel ──▶ Repository ──▶ Retrofit Api
   ▲                                    │                              │
   └── eventos (Channel/Flow) ──────────┘                              ▼
                                                                  AuthTokenStore
                                                                  (DataStore)
```

Reglas:
- **UI (Compose)** nunca conoce Retrofit ni DataStore. Solo habla con su `ViewModel`.
- **ViewModel** expone `StateFlow` para estado de pantalla y un `Channel`/`Flow` para eventos one-shot (navegación, snackbars).
- **Repository** es el único que combina red y persistencia local. Devuelve `Result<T>` y mapea errores a `AuthException` con mensaje listo para mostrar.
- **Retrofit Apis** son interfaces puras con anotaciones. Sin lógica.

El grafo de dependencias se arma a mano en `MainActivity`. No hay Hilt todavía (ver [FAQ](#faq)).

---

## Estructura del proyecto

```
app/src/main/java/com/uniandes/travelhub/
├── MainActivity.kt              # Entry point, instancia el repo y monta AuthNavGraph
├── models/                      # DTOs (request/response) y enums de dominio
│   ├── UserRole.kt
│   └── auth/                    # LoginRequest/Response, RegisterRequest, TokenResponse, ...
├── network/                     # Capa HTTP
│   ├── RetrofitFactory.kt       # Singleton Retrofit + OkHttp + logging en debug
│   ├── SecurityApi.kt           # Endpoints del microservicio de seguridad
│   ├── UsersApi.kt              # Endpoints del microservicio de usuarios
│   ├── AuthTokenStore.kt        # DataStore Preferences para token/role/locale
│   └── ApiErrorParser.kt        # HttpException → mensaje legible (FastAPI-style)
├── repositories/
│   └── AuthRepository.kt        # Orquesta SecurityApi + UsersApi + TokenStore
├── viewmodels/                  # Un VM por pantalla
│   ├── LoginViewModel.kt
│   ├── RegisterViewModel.kt
│   ├── VerifyOtpViewModel.kt
│   └── ErrorMessage.kt          # sealed: Resource(@StringRes) | Plain(String)
├── ui/
│   ├── theme/                   # Material3 theme, colores, tipografía, spacing
│   └── auth/
│       ├── navigation/          # AuthRoute (rutas) + AuthNavGraph (NavHost)
│       ├── login/ register/ verifyotp/ home/
│       └── components/          # TextField, Button, Banner, LanguageSwitcher, ...
└── utils/
    ├── AuthValidators.kt        # Validación de email/password
    └── PasswordStrength.kt

app/src/main/res/
├── values/strings.xml           # Español (default)
├── values-en/strings.xml        # Inglés
├── values-pt/strings.xml        # Portugués
└── xml/locales_config.xml       # Locales soportados (per-app language)
```

---

## Cómo extender el proyecto

### Agregar un endpoint a un microservicio existente

Ejemplo: `GET /api/v1/users/me`.

1. **DTO** en `models/auth/`:
   ```kotlin
   data class MeResponse(val id: String, val email: String, val role: UserRole)
   ```
   Si usas codegen de Moshi, anota con `@JsonClass(generateAdapter = true)` (KSP ya está habilitado).

2. **Endpoint** en la interfaz Retrofit (`network/UsersApi.kt`):
   ```kotlin
   @GET("api/v1/users/me")
   suspend fun getMe(@Header("Authorization") bearer: String): MeResponse
   ```

3. **Método en el repository** (`repositories/AuthRepository.kt`):
   ```kotlin
   suspend fun getMe(token: String): Result<MeResponse> = runCatching {
       usersApi.getMe("Bearer $token")
   }.recoverFailure("No fue posible cargar el perfil")
   ```
   Reutiliza `recoverFailure` para que los errores HTTP se conviertan automáticamente en `AuthException` con el mensaje del backend.

4. **Consumirlo desde un ViewModel** dentro de `viewModelScope.launch { ... }` y actualizar el `StateFlow` correspondiente.

### Agregar un microservicio nuevo

1. Añade la URL base en `local.properties.example` y `local.properties`.
2. En `app/build.gradle.kts`, léela y declara un `buildConfigField`:
   ```kotlin
   val ordersApiBase: String = localProperties.getProperty("TRAVELHUB_ORDERS_API_BASE", "http://10.0.2.2:8002/")
   buildConfigField("String", "ORDERS_API_BASE", "\"$ordersApiBase\"")
   ```
3. Crea la interfaz `network/OrdersApi.kt`.
4. Exponla en `RetrofitFactory`:
   ```kotlin
   val ordersApi: OrdersApi by lazy { build(BuildConfig.ORDERS_API_BASE) }
   ```
   Reusa el `okHttpClient` existente para mantener un único pool de conexiones.

### Agregar una pantalla nueva

1. **Ruta** en `ui/auth/navigation/AuthRoute.kt` (o `ui/<feature>/navigation/` si es otra feature):
   ```kotlin
   data object Profile : AuthRoute("profile")
   ```
   Si la ruta lleva argumentos, sigue el patrón de `VerifyOtp` (`route` con `{arg}` y un helper `build(value)`).

2. **ViewModel** en `viewmodels/`:
   - Hereda de `androidx.lifecycle.ViewModel`.
   - Expone `StateFlow` para estado y, si hace falta, un `Channel<Event>` para acciones one-shot.
   - Incluye una `class Factory(...) : ViewModelProvider.Factory` igual que `LoginViewModel.Factory`.

3. **Pantalla** en `ui/<feature>/<pantalla>/`. Convención del repo:
   - `XxxScreen.kt`: composable con efectos (lee `uiState`, observa `events`, navega). Recibe el `ViewModel`.
   - `XxxScreenContent` (composable interno o archivo aparte): **función pura** sin dependencias del VM, recibe estado y callbacks. Esto es lo que se testea con `createComposeRule()`.

4. **Registrar el destino** en `AuthNavGraph.kt` con un bloque `composable(...)` que cree el VM con `viewModel(factory = ...)` y delegue navegación al `NavHostController`.

5. **Strings**: añade las claves en `res/values/strings.xml` (es), `values-en/strings.xml` y `values-pt/strings.xml`. Las tres deben tener exactamente las mismas keys.

### Agregar mensajes de error

- Si el mensaje viene del backend → devuelve `ErrorMessage.Plain(textoDelBackend)`. `ApiErrorParser` ya extrae `detail` (string o lista FastAPI-style).
- Si el mensaje es del cliente (validación) → declara la string en `res/values*/strings.xml` y devuelve `ErrorMessage.Resource(R.string.mi_error)`.
- En la UI, resuelve con `ErrorMessageResolver` para obtener el `String` final.

### Agregar validaciones

Pon la lógica pura en `utils/` (como `AuthValidators` o `PasswordStrength`) — sin dependencias de Android. Eso permite testearla con JUnit puro y reusarla desde cualquier VM.

### Persistir un nuevo dato local

`AuthTokenStore` ya envuelve un `DataStore<Preferences>` singleton:

1. Declara la `Preferences.Key` en el `companion object`.
2. Expón un `Flow<...>` con `dataStore.data.map { ... }`.
3. Crea una función `suspend` que use `dataStore.edit { prefs -> prefs[KEY] = value }`.

Si el dato no es de auth, considera crear un store nuevo (p.ej. `network/SettingsStore.kt`).

---

## Convenciones

- **Paquetes** por capa, no por feature (excepto `ui/<feature>/`).
- **Nombres**: `XxxScreen` (composable con VM) · `XxxScreenContent` (puro) · `XxxViewModel` · `XxxApi` · `XxxRepository`.
- **Estados de pantalla**: `sealed interface XxxUiState { Idle | Loading | Error(message: ErrorMessage) | Success(...) }`.
- **Eventos one-shot**: `Channel<XxxEvent>(Channel.BUFFERED)` expuesto como `receiveAsFlow()`.
- **Errores**: siempre via `Result<T>` desde el repo. La UI nunca atrapa excepciones de red.
- **i18n**: todo texto visible debe ser una `string resource`. No hardcodees español en código.
- **Theme**: usa tokens (`MaterialTheme.colorScheme`, `Spacing`, `ExtendedColors`) en vez de literales `Color(0xFF...)` o `dp` mágicos.

---

## Build y debugging

```bash
./gradlew assembleDebug    # genera el APK debug
./gradlew installDebug     # instala en el emulador/dispositivo
./gradlew lint             # análisis estático
./gradlew clean            # limpia build/
```

- **Logging HTTP**: `RetrofitFactory` agrega un `HttpLoggingInterceptor` con `Level.BODY` solo cuando `BuildConfig.DEBUG`. Las requests/responses aparecen en Logcat con tag `OkHttp`.
- **Network security**: `app/src/main/res/xml/network_security_config.xml` permite cleartext hacia hosts de desarrollo (`10.0.2.2`, `localhost`). En producción debe usarse HTTPS.
- **Idioma de la app**: `MainActivity` lee `AppCompatDelegate.getApplicationLocales()` y lo persiste con `AppCompatDelegate.setApplicationLocales(...)`. Locales soportados en `res/xml/locales_config.xml`.

---

## Checklist de PR

- [ ] `./gradlew testDebugUnitTest` pasa en local.
- [ ] Si tocaste UI, hay test de `XxxScreenContent` con `createComposeRule()`.
- [ ] Si tocaste un VM, hay test con `MainDispatcherRule` + MockK + Turbine.
- [ ] Si tocaste un endpoint, hay test con `MockWebServer`.
- [ ] Strings nuevas existen en los **tres** locales (`values/`, `values-en/`, `values-pt/`).
- [ ] Ningún literal hardcoded en castellano en código de UI.
- [ ] No se commitearon `local.properties` ni claves.
- [ ] Si añadiste un endpoint con auth, se inyecta vía header `Authorization: Bearer ...` (el token está en `AuthTokenStore.tokenFlow`).

---

## FAQ

**¿Por qué no hay Hilt/Koin?** El grafo es pequeño y se arma a mano en `MainActivity`. Si crece el número de repositorios/usecases, vale la pena introducir Hilt.

**¿Por qué los Screen están separados en `Screen` y `ScreenContent`?** Para poder testear la UI con `createComposeRule()` sin instanciar el ViewModel real. El `Screen` lee `uiState` y delega callbacks; el `Content` solo recibe datos.

**¿Por qué Robolectric en unit tests en vez de instrumentation?** Velocidad. Compose UI tests con Robolectric corren en JVM en segundos, sin emulador. Solo usa `androidTest/` cuando necesites el dispositivo real (sensores, Camera, integraciones nativas).

**¿Dónde defino un nuevo color/spacing/tipografía?** En `ui/theme/Color.kt`, `Spacing.kt`, `Type.kt`, `ExtendedColors.kt`. Nunca hardcodees `Color(0xFF...)` o `dp` mágicos en pantallas.
