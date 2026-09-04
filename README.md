# Учёт расходов

[![CI](https://github.com/oleg-belyanin/expense-tracker/actions/workflows/ci.yml/badge.svg)](https://github.com/oleg-belyanin/expense-tracker/actions/workflows/ci.yml)
[![Debug APK](https://img.shields.io/github/v/release/oleg-belyanin/expense-tracker?label=debug%20apk)](https://github.com/oleg-belyanin/expense-tracker/releases/latest)

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
:core:categorization  TextNormalizer + Snowball 3.1.1, CategorizationEngine (§10.1)
:core:domain          use case и интерфейсы repository
:core:database        Room 3, Bundled SQLite, FTS5, seed import
:seed-generator       CLI: seed-артефакт и демо-расходы (не путать)
```

Встроенные категории — [`seed-data/categories.yaml`](seed-data/categories.yaml).
Seed-артефакт: [`app/src/main/assets/seed/`](app/src/main/assets/seed/).

## Стек и почему

Kotlin, Jetpack Compose и Material 3 — один язык от домена до UI и прямое
соответствие кадрам Figma без XML-экранов. Room 3 + Bundled SQLite / FTS5 —
одинаковый движок на устройствах и поиск < 300 мс на 5 000 записей. Navigation 3
только для вкладок, формы расхода и настроек; панели остаются sheet/dialog.
Без Hilt/Koin: явный `AppContainer`. Диаграммы — Vico и Canvas на готовой
доменной модели. DataStore хранит только тему.

## Решения, где ТЗ оставляло свободу

- Валюта ₽, UI на русском. Будущая дата в расходе запрещена (даты после сегодня
  в календаре не выбираются).
- Удаление категории — архив, не hard delete: расходы и правила сохраняют
  `category_id`. «Прочее» не архивируется.
- Quick Add — полноэкранный destination, не bottom sheet: так собран кадр Figma.
- Настройки открываются шестерёнкой с любой вкладки, без четвёртого пункта навбара.
- Тема: система / светлая / тёмная, переживает перезапуск.
- Демо-300 расходов только в debug APK и только при первом запуске; очистка
  истории повторно их не подставляет.

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
Новые тесты B3/B6 (нормализатор, движок, seed, агрегация, CSV/JSON)
входят в тот же `check` — отдельная команда в CI не нужна.
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
./gradlew :seed-generator:generateDemo
./gradlew :seed-generator:generateNfrDemo
```

`installDebug` ставит APK на подключённое устройство или эмулятор.

Чистый клон собирается без правок исходников: достаточно JDK 17, SDK
и `local.properties` / `ANDROID_HOME` (NFR-9).

## CI

Каждый push, pull request и ручной запуск workflow:

1. JDK 17 + Android SDK + кэш Gradle;
2. `./gradlew check` (ktlint, Android Lint, JVM-тесты);
3. `./gradlew :seed-generator:generateSeed` и сверка `app/src/main/assets/seed/`
   с генерацией;
4. `./gradlew :app:assembleDebug`;
5. debug APK как artifact (хранение 7 дней);
6. тот же APK в [Releases](https://github.com/oleg-belyanin/expense-tracker/releases/latest)
   после успешного прогона на `main`;
7. отчёты `check` как artifact `check-reports` (даже если шаг красный).

В CI нет эмулятора, instrumentation, `assembleRelease` и публикации в магазины.
Падение теста или lint делает PR красным.

Debug APK без локальной машины:

- **[Releases → Debug APK](https://github.com/oleg-belyanin/expense-tracker/releases/latest)**
  — актуальная сборка, файл `app-debug.apk`;
- или **Actions → CI → успешный прогон → Artifacts → `app-debug`** (7 дней).

```bash
gh release download debug-apk --repo oleg-belyanin/expense-tracker --pattern '*.apk'
# либо артефакт прогона:
gh run download --name app-debug --repo oleg-belyanin/expense-tracker
```

## Эмулятор

Полная инструкция: [`docs/emulator.md`](docs/emulator.md) (AVD `ExpenseTracker_360`, 360×800).

```bash
export ANDROID_HOME=~/Android/Sdk
$ANDROID_HOME/emulator/emulator -avd ExpenseTracker_360 &
./gradlew :app:installDebug
```

## Категоризация

Это не ML: детерминированный вероятностный словарь из размеченных примеров.
Один `TextNormalizer` (NFKC, ё→е, стоп-слова, русский Snowball) у приложения
и у `:seed-generator`.

Последовательность решения (§10.1):

1. пользовательское exact rule;
2. alias по имени категории;
3. seed exact rule;
4. средний вектор слов названия, затем смесь с вектором места;
5. fallback «Прочее».

Пока слово набирается, токен ищется по префиксу в том же словаре (как места:
«Ригл» → «Ригла»). Категория подставляется, если префикс однозначен; иначе
показываются подсказки названия из истории и словаря.

Cold start: 800 train + 200 validation строк в [`seed-data/raw/`](seed-data/raw/).
Генерация артефакта:

```bash
./gradlew :seed-generator:generateSeed
```

Записанные параметры — [`seed-data/categorization-config.json`](seed-data/categorization-config.json)
(копия в assets). Validation seed v1: top-1 **85 %**, fallback **10 %**.

## Демо-данные и NFR-2

`seed-data/` — только словарь категоризации, без сумм и дат.
Демо для списка и аналитики — [`demo-data/expenses-ui.csv`](demo-data/expenses-ui.csv):
300 расходов в формате экспорта F-09, имена взяты из train.csv.

Debug-сборка кладёт копию в `app/src/debug/assets/demo/` и при первом запуске
импортирует её, если расходов ещё нет. Очистка истории повторно демо не подставляет.
Release-APK файла не содержит — пустой список остаётся пустым.

```bash
./gradlew :seed-generator:generateDemo
./gradlew :seed-generator:generateNfrDemo
```

Набор на 5 000 записей (NFR-2) пишется в `demo-data/local/` и не коммитится.
Чтобы прогнать его на эмуляторе, укажите этот CSV как `--output` debug-assets
и переустановите debug APK.

## Миграции (F-08)

Схема Room версии 1 экспортируется в `core/database/schemas/`.
Список миграций — `AppMigrations.all`; `fallbackToDestructiveMigration` нет.
Неизвестная версия падает, данные не стираются. Следующий релиз добавляет
`Migration(1, 2)`, поднимает `@Database(version)` и коммитит `2.json`.

## Экспорт, копия и дедупликация

Экспорт (F-09) — CSV расходов: дата, сумма (рубли и копейки), название,
категория и её код, место, комментарий, источник назначения, `dedup_key`.

Резервная копия (F-10) — один JSON с полем `format=expense-tracker-backup`:
расходы, категории, места, пользовательские exact rules и контексты имён,
learning examples, транзиты, пользовательские агрегаты, версии схемы,
нормализатора и seed. Seed-счётчики в файл не дублируются.

Восстановление сначала проверяет весь файл. Запись идёт одной транзакцией.
Дубликаты пропускаются по `expense.id` (UUID) и `dedup_key`. Повтор той же
копии не удваивает историю и примеры. Битый или чужой файл даёт понятную
ошибку, существующие данные не меняются.

Дедупликация импорта (F-08): `import:<имя>|<spentAt>|<amount>|<место>`.
Расходы, введённые вручную, получают `user:<uuid>`.

Очистка истории (F-11) удаляет расходы и обнуляет `learning_example.expense_id`.
Пользовательские категории и правила категоризации остаются.

## Известные ограничения

- Полный чеклист §5 ТЗ на телефоне и 2–4 минуты видео — этап I4, после F7.
  Пока проверено на AVD `ExpenseTracker_360` (API 34, 360×800). NFR-1/NFR-2
  на среднем физическом устройстве не замерялись.
- Instrumentation и эмулятор в CI нет — только `./gradlew check` и сборка APK.
- Бонусы B-01…B-03 не делались. B-04 закрыт юнит-тестами категоризации/агрегации
  и GitHub Actions.
- Пересчёт категорий после смены seed есть в домене, отдельной кнопки в UI нет:
  пользовательские exact rule не перезаписываются импортом.

## Документация

| Документ | Содержание |
|---|---|
| [`docs/testassignmentexpensetracker.md`](docs/testassignmentexpensetracker.md) | Требования и приёмка |
| [`docs/categorization-architecture.md`](docs/categorization-architecture.md) | Алгоритм категоризации и поиск мест |
| [`docs/seed-dataset-plan.md`](docs/seed-dataset-plan.md) | Seed-датасет 1 000 строк, генератор, поставка |
| [`docs/implementation-work-plan.md`](docs/implementation-work-plan.md) | План работ по этапам |
| [`docs/emulator.md`](docs/emulator.md) | Запуск AVD, установка APK, первичная настройка |
