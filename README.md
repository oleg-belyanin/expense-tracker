# Учёт расходов

Локальное Android-приложение для учёта личных трат. Серверной части нет:
данные и правила категоризации живут на устройстве.

## Требования

- **JDK 17** (`JAVA_HOME` указывает на JDK, не на JRE)
- **Android SDK** с Platform 37.0 (`platforms;android-37.0`), Build-Tools 36.0.0 и **platform-tools** (`adb`)
- **Android Studio** (удобно для UI и Device Manager; сборка идёт и из CLI)
- Gradle Wrapper уже в репозитории — ставить Gradle отдельно не нужно

Версии библиотек и плагинов задаются только в [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Локальная машина

1. Установите JDK 17 (Temurin, OpenJDK или JBR 17) и задайте `JAVA_HOME`.
2. Установите Android Studio из [официального дистрибутива](https://developer.android.com/studio)
   или JetBrains Toolbox. SDK можно поставить и command-line tools:
   распакуйте [Command line tools](https://developer.android.com/studio#command-tools)
   в `~/Android/Sdk/cmdline-tools/latest` и выполните:

   ```bash
   sdkmanager --sdk_root="$HOME/Android/Sdk" \
     "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
   ```

3. Скопируйте шаблон и пропишите путь к SDK **или** задайте `ANDROID_HOME`:

   ```bash
   cp local.properties.example local.properties
   ```

   В `local.properties` раскомментируйте и поправьте `sdk.dir`.
   Файл не коммитится: в нём только локальный путь, без секретов.

4. Debug-сборка подписывается **стандартным debug keystore**
   (`~/.android/debug.keystore`). Релизный ключ и Play Console не настраиваются.

На этой машине после этапа I0:

| Инструмент | Путь |
|---|---|
| JDK 17 | `~/.local/jdk-17` |
| Android SDK | `~/Android/Sdk` |
| `adb` | `~/Android/Sdk/platform-tools/adb` |

## Сборка

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew lint
./gradlew :app:installDebug
```

`installDebug` ставит APK на подключённое устройство или эмулятор
(эмулятор `ExpenseTracker_360` — этап I1).

Чистый клон собирается без правок исходников: достаточно JDK 17, SDK
и `local.properties` / `ANDROID_HOME` (NFR-9).
