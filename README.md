# Registro de Incidencias

Aplicación Android nativa (Kotlin) para registrar incidencias técnicas de forma sencilla: descripción, prioridad (Baja/Media/Alta) y marca de tiempo, con un contador y listado en memoria durante la sesión.

## Stack técnico

- **Lenguaje:** Kotlin
- **UI:** Vistas XML (ViewBinding) + Material Components
- **Gradle:** 8.9 (wrapper incluido)
- **Android Gradle Plugin (AGP):** 8.7.3
- **Kotlin:** 1.9.24
- **compileSdk / targetSdk:** 34
- **minSdk:** 24

## Requisitos para ejecutarlo

- Android Studio (Ladybug/2024.2.1 o más reciente, compatible con AGP 8.7.3)
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
