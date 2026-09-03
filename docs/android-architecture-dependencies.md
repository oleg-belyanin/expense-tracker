---
id: AD-ANDROID-001
kind: architecture
title: Архитектура Android-приложения и зависимости
status: proposed
created: 2026-09-02
related_scope: SC-TA-001
related_architecture: AD-CAT-001
---

# Архитектура Android-приложения и зависимости

## 1. Назначение

Документ фиксирует стек, границы модулей и набор внешних зависимостей Android-приложения.
Состав ограничен MVP из [`SC-TA-001`](./testassignmentexpensetracker.md). Архитектура категоризации подробно описана отдельно
в [`AD-CAT-001`](./categorization-architecture.md).

Базовый стек:

- Kotlin;
- Jetpack Compose и Material 3;
- одно Android-приложение без серверной части;
- локальная SQLite через Room 3;
- coroutines и Flow;
- Gradle Version Catalog (`libs.versions.toml`) для фиксации версий.

Используются только стабильные версии библиотек, доступные на момент создания проекта.
Compose-библиотеки подключаются через Compose BOM. Версии остальных зависимостей фиксируются
в Version Catalog и обновляются централизованно.

## 2. Модули

```text
:core:model
    Доменные сущности и value objects. Не зависит от Android.

:core:categorization
    TextNormalizer, CategorizationEngine, модели правил и расчётов.
    Kotlin/JVM, без Android API и SQL.

:core:database
    Room database, DAO, FTS5, миграции, seed-данные и реализации repository.

:core:domain
    Use cases приложения: расходы, фильтры, аналитика, категории,
    экспорт, backup и восстановление.

:app
    Compose UI, навигация, ViewModel, тема и composition root.

:seed-generator
    Kotlin/JVM CLI для воспроизводимой подготовки seed-артефакта.
    Использует тот же :core:categorization, что и приложение.
    Датасет 1 000 строк и pipeline — [`seed-dataset-plan.md`](./seed-dataset-plan.md).
```

Направление зависимостей:

```text
:app ────────────────> :core:domain ─────> :core:model
  │                           │
  └──────────────────> :core:database ───> :core:categorization

:seed-generator ─────────────────────────> :core:categorization
```

`core:model`, `core:domain` и `core:categorization` не зависят от Compose. UI не обращается
к DAO напрямую. Связывание интерфейсов repository с реализациями выполняется в composition
root модуля `:app`.

## 3. Production-зависимости

### 3.1. UI и жизненный цикл

| Назначение | Зависимость |
|---|---|
| Согласованные версии Compose | `androidx.compose:compose-bom` |
| Базовый UI | `androidx.compose.ui:ui` |
| Layout и базовые элементы | `androidx.compose.foundation:foundation` |
| Компоненты и темы | `androidx.compose.material3:material3` |
| Предпросмотр | `androidx.compose.ui:ui-tooling-preview` |
| Activity host | `androidx.activity:activity-compose` |
| Lifecycle-aware Flow | `androidx.lifecycle:lifecycle-runtime-compose` |
| ViewModel в Compose | `androidx.lifecycle:lifecycle-viewmodel-compose` |

`ui-tooling` подключается только в `debugImplementation`.

### 3.2. Навигация

| Назначение | Зависимость |
|---|---|
| Back stack и типизированные ключи | `androidx.navigation3:navigation3-runtime` |
| Отображение destination | `androidx.navigation3:navigation3-ui` |
| ViewModel scope для destination | `androidx.lifecycle:lifecycle-viewmodel-navigation3` |

Navigation 3 используется только для экранов:

- расходы;
- редактирование расхода;
- аналитика;
- категории;
- настройки и файловые операции.

Быстрый ввод, фильтры, выбор категории и подтверждения реализуются через
`ModalBottomSheet` / `AlertDialog` Material 3 и не становятся отдельными destination.

### 3.3. Локальное хранение и поиск

| Назначение | Зависимость |
|---|---|
| Room runtime | `androidx.room3:room3-runtime` |
| Генерация Room-кода | `androidx.room3:room3-compiler` через KSP |
| Одинаковая SQLite и FTS5 на устройствах | `androidx.sqlite:sqlite-bundled` |

База создаётся с `BundledSQLiteDriver`. Это гарантирует доступность одинаковой версии SQLite
и FTS5 на всех поддерживаемых устройствах. Room отвечает за миграции, транзакции и DAO.

### 3.4. Асинхронность и настройки

| Назначение | Зависимость |
|---|---|
| Coroutines на Android | `org.jetbrains.kotlinx:kotlinx-coroutines-android` |
| Тема приложения | `androidx.datastore:datastore-preferences` |

Room DAO возвращают `Flow` для наблюдаемых данных и `suspend` для команд. DataStore хранит
только настройку темы `system | light | dark`; расходы, категории и правила в нём не хранятся.

### 3.5. Сериализация, экспорт и backup

| Назначение | Зависимость |
|---|---|
| JSON backup | `org.jetbrains.kotlinx:kotlinx-serialization-json` |

CSV-экспорт реализуется небольшим собственным writer-компонентом с корректным экранированием
полей. Отдельная CSV-библиотека для одного фиксированного формата не нужна.

Файлы выбираются и передаются через Android Storage Access Framework и системный Sharesheet.
Дополнительная библиотека файлового менеджера не используется.

### 3.6. Аналитика

| Назначение | Зависимость |
|---|---|
| Кольцевая и столбчатая диаграммы | Vico Compose |
| Интеграция с Material 3 | Vico Compose Material 3 |

Vico используется только как слой визуализации. Расчёт сумм, долей и периодов остаётся
в `:core:domain`. Обе диаграммы получают одну и ту же доменную модель, что исключает
расхождение данных между представлениями F-06.

Если стабильная версия Vico с pie/donut API окажется несовместима с выбранной версией Compose,
кольцевая диаграмма реализуется через Compose `Canvas`, а Vico остаётся только для столбцов.
Экспериментальный API не должен становиться обязательной частью доменного слоя.

### 3.7. Нормализация русского текста

Русский Snowball stemmer используется как исходный код внутри `:core:categorization`,
а не как полный Lucene dependency.

Исходник stemmer:

- копируется из официально сгенерированной реализации Snowball
  (`libstemmer_java-3.1.1`: runtime + `russianStemmer`);
- сохраняет исходную лицензию и ссылку на upstream
  (`core/categorization/third_party/snowball/`);
- не переписывается вручную;
- вызывается общим `TextNormalizer` и в приложении, и в `:seed-generator`.

Так приложение не получает тяжёлую зависимость от Lucene, а production и seed pipeline
используют идентичную нормализацию.

## 4. Dependency injection

DI-фреймворк в MVP не используется.

Зависимости создаются в composition root:

```text
Application
└── AppContainer
    ├── AppDatabase
    ├── repositories
    ├── CategorizationEngine
    └── use cases
```

ViewModel получают зависимости через явные factory. Для текущего числа экранов это проще
и прозрачнее Hilt/Koin, не добавляет annotation processing сверх уже необходимого KSP Room
и хорошо показывает граф зависимостей на техническом разборе.

Если число feature-модулей и scoped-зависимостей существенно вырастет, DI-фреймворк можно
добавить отдельным архитектурным решением.

## 5. Тестовые зависимости

| Назначение | Зависимость |
|---|---|
| Unit-тесты | JUnit |
| Тестирование coroutines и Flow | `kotlinx-coroutines-test` |
| Assertions | стандартные JUnit/Kotlin assertions |
| Compose UI tests | `androidx.compose.ui:ui-test-junit4` |
| Тестовый manifest/activity | `androidx.compose.ui:ui-test-manifest` в debug |

Основной объём тестов выполняется на JVM:

- нормализация;
- приоритеты CategorizationEngine;
- обучение и идемпотентность;
- агрегация аналитики;
- валидация периода и суммы;
- CSV/JSON round trip.

Инструментальные тесты покрывают только критические UI-потоки и интеграцию Room/FTS.
Mocking-фреймворк в первой версии не требуется: repository подменяются небольшими fake-классами.

## 6. Зависимости, которые сознательно не используются

| Зависимость | Причина |
|---|---|
| Retrofit / OkHttp | Сервер и сеть отсутствуют в MVP |
| WorkManager | Нет обязательной фоновой синхронизации или очереди |
| Hilt / Koin | Граф достаточно мал для явного composition root |
| Paging 3 | 5 000 локальных записей обслуживаются Room query + lazy list |
| Coil / Glide | В MVP нет сетевых или пользовательских изображений |
| Firebase / analytics SDK | Приватность и offline-first; продуктовая аналитика не требуется |
| SQLCipher | Шифрование базы не входит в ТЗ |
| Lucene | Для одного Snowball stemmer зависимость избыточна |
| Accompanist | Нужные API доступны в Compose/Material 3 |
| Material Icons Extended | Иконки категорий поставляются локальными vector assets |
| MPAndroidChart | UI реализуется на Compose без View interoperability |
| AndroidX Navigation 2 | Новый проект использует Navigation 3 |

## 7. Соответствие требованиям

| Требование | Архитектурное решение |
|---|---|
| F-01, F-05 | Compose, ViewModel, Flow, LazyColumn |
| F-02 | Room category DAO, soft delete из AD-CAT-001 |
| F-03, F-04 | `:core:categorization`, Snowball, Room rules/statistics |
| F-05 | Room FTS5 и `BundledSQLiteDriver` |
| F-06, F-07 | Domain analytics + Vico/Canvas + Navigation 3 |
| F-08 | Room 3, локальные транзакции и миграции |
| F-09 | Собственный CSV writer + Sharesheet |
| F-10 | Kotlin Serialization + Storage Access Framework |
| F-11 | Раздельный жизненный цикл расходов и learning examples |
| F-12 | Material 3 theme + DataStore Preferences |
| NFR-2 | Индексированные Room-запросы, FTS5 и Compose lazy list |
| NFR-4, NFR-6 | Отсутствие сетевых SDK и серверных зависимостей |
| NFR-8 | KSP, статический анализ и unit/UI tests |

## 8. Правила добавления новых зависимостей

Новая библиотека добавляется только если одновременно:

1. закрывает требование из `SC-TA-001`;
2. заметно снижает сложность или риск по сравнению с небольшой собственной реализацией;
3. имеет стабильный релиз и совместима с выбранными Kotlin, Compose и Android Gradle Plugin;
4. не требует сети для основного сценария;
5. не переносит бизнес-логику из domain-слоя во внешний UI/SDK;
6. её назначение и причина использования добавлены в этот документ.

Все версии задаются только в `libs.versions.toml`. Версии не дублируются в `build.gradle.kts`.
