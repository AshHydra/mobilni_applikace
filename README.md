# Mobilní aplikace – závěrečný projekt (Kotlin, MVVM)

Tento repozitář obsahuje Android aplikaci psanou v Kotlinu s architekturou **MVVM**, s UI v **Jetpack Compose**, s **navigací mezi obrazovkami**, **síťovou komunikací** (REST) a **perzistencí** (Room).

## Požadavky

- Android Studio (kvůli Android SDK/emulátoru) *nebo* nainstalované Android SDK v systému
- JDK 17+ (u tebe je dostupné JDK 21)

## Build APK (debug)

Ve Windows PowerShell / terminálu v rootu projektu:

```bash
.\gradlew.bat assembleDebug
```

Výsledné APK najdeš v:

`app\build\outputs\apk\debug\app-debug.apk`

## Co aplikace dělá

- Načte seznam příspěvků z veřejného REST API (`jsonplaceholder.typicode.com`)
- Zobrazí seznam (Home), detail a obrazovku oblíbených (Favorites)
- Umožní přidat/odebrat oblíbené položky a ukládá je do databáze (Room)

