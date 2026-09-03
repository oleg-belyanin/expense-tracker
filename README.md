# Учёт расходов

[![CI](https://github.com/oleg-belyanin/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/oleg-belyanin/expense-tracker/actions/workflows/ci.yml)

Локальное Android-приложение для учёта личных трат. Серверной части нет:
данные и правила категоризации живут на устройстве.

## Требования

- **JDK 17** (`JAVA_HOME` указывает на JDK, не на JRE)
- **Android SDK** с Platform 37.0 (`platforms;android-37.0`), Build-Tools 36.0.0 и **platform-tools** (`adb`)
- **Android Studio** (удобно для UI и Device Manager; сборка идёт и из CLI)
- Gradle Wrapper уже в репозитории — ставить Gradle отдельно не нужно

Версии библиотек и плагинов задаются только в [`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Модули

```text
:app                  UI, Application, AppContainer
:core:model           Expense, Category, Location, Money, Period
:core:categorization  TextNormalizer + русский Snowball 3.1.1 (не Lucene)
:core:domain          use case и интерфейсы repository
:core:database        Room 3, Bundled SQLite, FTS5, seed import
```

`:seed-generator` появится на этапе B3. Встроенные категории — [`seed-data/categories.yaml`](seed-data/categories.yaml).
Stub seed: [`app/src/main/assets/seed/`](app/src/main/assets/seed/).

## Категории (F-02)

10 встроенных категорий поставляются с приложением. Пользователь создаёт и правит
только свои: имя, один из 7 цветов, глиф из каталога. Иконка подбирается по
нормализованному имени (`Питомцы` → `pets`); если каталог не подошёл — `letter`.

Удаление — soft delete (`archived_at`). Расходы и правила категоризации сохраняют
`category_id`, записи не пропадают. Встроенные категории тоже можно архивировать,
кроме защищённой «Прочее». Повторное создание того же нормализованного имени
реактивирует ту же строку (тот же `id`). Архивные категории не предлагаются
в новых расходах, но остаются в истории и отчётах.

Создание категории даёт category alias (полное совпадение нормализованного имени
покупки) и производную статистику `source=category_name` для слов имени.

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

## Сборка и проверка

Обязательный локальный шлюз после каждого этапа (NFR-8):

```bash
./gradlew check
```

`check` = ktlint + Android Lint + JVM unit-тесты всех модулей.
Инструментальные тесты на эмуляторе в `check` не входят.

Порог — ноль ошибок. Baseline Lint не заведён: свой код должен быть чистым.
Предупреждения Lint про pinned AGP/targetSdk допустимы, ошибки — нет.

Отдельные команды:

```bash
./gradlew :app:assembleDebug
./gradlew test
./gradlew lint
./gradlew ktlintCheck
./gradlew ktlintFormat
./gradlew :app:installDebug
```

`installDebug` ставит APK на подключённое устройство или эмулятор.

Чистый клон собирается без правок исходников: достаточно JDK 17, SDK
и `local.properties` / `ANDROID_HOME` (NFR-9).

## CI

Каждый push, pull request и ручной запуск workflow:

1. JDK 17 + Android SDK + кэш Gradle;
2. `./gradlew check` (ktlint, Android Lint, JVM-тесты);
3. после этапа B3 — `./gradlew :seed-generator:generateSeed` и сверка
   `app/src/main/assets/seed/` с генерацией;
4. `./gradlew :app:assembleDebug`;
5. debug APK как artifact (хранение 7 дней).

В CI нет эмулятора, instrumentation, `assembleRelease` и публикации в магазины.
Падение теста или lint делает PR красным.

Debug APK без локальной машины: **Actions → CI → успешный прогон →
Artifacts → `app-debug`**. Либо:

```bash
gh run download --name app-debug --repo oleg-belyanin/expense-tracker
```

## Эмулятор (AVD `ExpenseTracker_360`)

Основной AVD для ежедневной разработки и инструментальных тестов.
Соответствует макету Figma: **360×800 dp**, **420 dpi** (945×2100 px).

| Параметр | Значение |
|---|---|
| Имя AVD | `ExpenseTracker_360` |
| API | 34, Google APIs, x86_64 |
| Базовый профиль | Pixel 4 (punch-hole в рамке) |
| RAM | 3 ГБ |
| Навигация | жестовая (настроить один раз в эмуляторе) |

### Создание AVD

Нужны пакеты `emulator` и `system-images;android-34;google_apis;x86_64`.
Скрипт ставит их при необходимости и создаёт AVD:

```bash
export JAVA_HOME=~/.local/jdk-17   # или ваш путь к JDK 17
export ANDROID_HOME=~/Android/Sdk
./scripts/create-expense-tracker-avd.sh
```

Через Device Manager в Android Studio: **Pixel 4**, API 34 Google APIs x86_64,
имя `ExpenseTracker_360`, затем в `~/.android/avd/ExpenseTracker_360.avd/config.ini`
задать `hw.lcd.width=945`, `hw.lcd.height=2100`, `hw.lcd.density=420`,
`hw.ramSize=3072`.

### Запуск и установка

```bash
export ANDROID_HOME=~/Android/Sdk
$ANDROID_HOME/emulator/emulator -avd ExpenseTracker_360 &
./gradlew :app:installDebug
```

Первый запуск на новом AVD:

1. **Settings → System → Gestures → System navigation → Gesture navigation**
2. При необходимости safe area: **Developer options → Simulate a display with a cutout → Punch hole**

На эмуляторе проверяют режим полёта (F-08), системную светлую/тёмную тему
и SAF для экспорта/backup. Инструментальные тесты Room/FTS и Compose UI
гоняют только на этом AVD, не в CI.

## Документация

| Документ | Содержание |
|---|---|
| [`docs/testassignmentexpensetracker.md`](docs/testassignmentexpensetracker.md) | Требования и приёмка |
| [`docs/categorization-architecture.md`](docs/categorization-architecture.md) | Алгоритм категоризации и поиск мест |
| [`docs/seed-dataset-plan.md`](docs/seed-dataset-plan.md) | Seed-датасет 1 000 строк, генератор, поставка |
| [`docs/implementation-work-plan.md`](docs/implementation-work-plan.md) | План работ по этапам |
