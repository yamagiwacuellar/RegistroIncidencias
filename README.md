# Registro de Incidencias

Aplicación Android nativa (Kotlin) para registrar incidencias técnicas de forma sencilla: título, descripción, prioridad (Baja/Media/Alta) y marca de tiempo, con retroalimentación inmediata, contador y listado en memoria durante la sesión.

## Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Vistas XML (ViewBinding) + Material Components
- **Gradle:** 8.13 (wrapper incluido)
- **Android Gradle Plugin (AGP):** 8.13.2
- **Kotlin:** 1.9.24
- **compileSdk / targetSdk:** 34
- **minSdk:** 24

## Requisitos para ejecutarlo

- Android Studio (Ladybug/2024.2.1 o más reciente)
- JDK 17 o superior (se recomienda usar el JDK embebido de Android Studio)
- Android SDK Platform 34 y Build-Tools correspondientes instalados desde el SDK Manager
- Un emulador (AVD) con API 24 o superior

## Cómo ejecutarlo

1. Abrir la carpeta del proyecto en Android Studio (`File > Open`).
2. Esperar a que finalice el Gradle Sync (la primera vez descargará dependencias).
3. Seleccionar un emulador en la barra de dispositivos.
4. Presionar **Run ▶** (Shift+F10).

## Estructura del proyecto

```
app/
 └─ src/main/
     ├─ java/com/example/registroincidencias/MainActivity.kt
     ├─ res/layout/activity_main.xml
     └─ res/values/ (strings, colors, themes)
```

## Notas

- `local.properties` no se versiona (contiene la ruta local del SDK); Android Studio lo regenera automáticamente al abrir el proyecto.

## Historial de avances

### Semana 6 — Interfaz con estado

- Se agregó un segundo campo de entrada (**Título de la incidencia**) además del de descripción existente.
- Ambos campos se validan de forma independiente antes de registrar (título vacío y descripción vacía muestran su propio error).
- Se agregó un mensaje de retroalimentación inmediata (`txtMensaje`) que arranca en *"Aún no hay reporte creado"* y cambia a *"Reporte preparado: `<título>`"* apenas se presiona **Registrar incidencia**, usando variables de estado en Kotlin (`titulo`, `descripcion`) que se leen del `ViewBinding` y se reflejan al instante en la vista — el equivalente, en un proyecto basado en Vistas XML, a `remember { mutableStateOf(...) }` en Compose.
- El listado y el contador (funcionalidad ya existente) ahora muestran también el título de cada incidencia registrada.
