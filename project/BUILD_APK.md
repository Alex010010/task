# Generar el APK con GitHub Actions

Este proyecto incluye un workflow en `.github/workflows/build-apk.yml`.

## Pasos

1. Crea un repositorio en GitHub.
2. Sube todo el contenido de este proyecto a la rama `main`.
3. En GitHub abre `Actions`.
4. Selecciona `Build Android APK`.
5. Pulsa `Run workflow`.
6. Cuando termine, abre la ejecución y descarga el artefacto `tareas-debug-apk`.
7. Dentro encontrarás `app-debug.apk`.

El APK debug es instalable directamente en un dispositivo Android para pruebas.

## Build local equivalente

El proyecto usa Android Gradle Plugin 8.6.1, por lo que Gradle 8.7 es una combinación válida.

```bash
gradle :app:assembleDebug
```

El resultado será:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para un APK release firmado será necesario añadir un keystore y configuración de signing.
