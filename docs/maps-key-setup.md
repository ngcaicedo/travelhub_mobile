# Google Maps API key — setup para búsqueda en mapa

La pantalla *Mapa de búsqueda* renderiza Google Maps a través de `maps-compose`.
Sin una API key válida el mapa se carga en blanco con un watermark de "API key
required". Sigue estos pasos para obtener una y restringirla.

## 1. Crear / reutilizar proyecto en Google Cloud

1. Entra a https://console.cloud.google.com.
2. Crea un proyecto (o selecciona uno existente).
3. Asegúrate de tener una cuenta de facturación asociada (Maps tiene un
   *free tier* de 28k cargas mensuales — suficiente para desarrollo).

## 2. Habilitar Maps SDK for Android

`APIs & Services → Library → Maps SDK for Android → Enable`.

## 3. Crear la API key

`APIs & Services → Credentials → + Create credentials → API key`.

Copia el valor — es lo que va en `local.properties` como `MAPS_API_KEY`.

## 4. Restringir la key (obligatorio antes de compartir o desplegar)

En la lista de credenciales, abre la key recién creada.

### Application restrictions
- *Android apps*
- Click *Add an item*:
  - **Package name:** `com.uniandes.travelhub`
  - **SHA-1 certificate fingerprint:** ejecuta lo siguiente desde la raíz del repo:

```bash
./gradlew signingReport | sed -n '/Variant: debug/,/SHA-256/p'
```

  Copia la línea `SHA1: AA:BB:CC:...` y pégala en el campo. Repite para la
  variant *release* cuando se necesite (puedes añadir múltiples entradas).

### API restrictions
- *Restrict key*
- Selecciona únicamente *Maps SDK for Android*.

Guarda. La propagación de las restricciones tarda 1-2 minutos.

## 5. Configurar el proyecto

1. Copia `local.properties.example` a `local.properties` (gitignored).
2. Pega la key:

```properties
MAPS_API_KEY=AIza...
```

3. `./gradlew assembleDebug` — el plugin de Android la inyecta como
   `manifestPlaceholders["MAPS_API_KEY"]` y aparece en `AndroidManifest.xml`
   como `<meta-data android:name="com.google.android.geo.API_KEY" .../>`.