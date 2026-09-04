# Эмулятор Android (AVD `ExpenseTracker_360`)

Основной AVD для ежедневной разработки и инструментальных тестов.
Соответствует макету Figma: **360×800 dp**, **420 dpi** (945×2100 px).

| Параметр | Значение |
|---|---|
| Имя AVD | `ExpenseTracker_360` |
| API | 34, Google APIs, x86_64 |
| Базовый профиль | Pixel 4 (punch-hole в рамке) |
| RAM | 3 ГБ |
| Навигация | жестовая (настроить один раз в эмуляторе) |

## Что нужно заранее

1. **JDK 17** — задайте `JAVA_HOME` (на этой машине: `~/.local/jdk-17`).
2. **Android SDK** — задайте `ANDROID_HOME` (обычно `~/Android/Sdk`) или пропишите
   `sdk.dir` в `local.properties` (см. [`README.md`](../README.md)).
3. Пакеты SDK: `emulator`, `platform-tools`,
   `system-images;android-34;google_apis;x86_64`.

```bash
export JAVA_HOME=~/.local/jdk-17   # или ваш путь к JDK 17
export ANDROID_HOME=~/Android/Sdk
```

## Создание AVD (один раз)

Скрипт ставит недостающие пакеты и создаёт AVD с нужным разрешением:

```bash
./scripts/create-expense-tracker-avd.sh
```

Проверить, что AVD появился:

```bash
$ANDROID_HOME/emulator/emulator -list-avds
```

В списке должен быть `ExpenseTracker_360`.

### Вручную через Android Studio

Device Manager → **Pixel 4**, API 34 Google APIs x86_64, имя `ExpenseTracker_360`.
Затем в `~/.android/avd/ExpenseTracker_360.avd/config.ini` задать:

```ini
hw.lcd.width=945
hw.lcd.height=2100
hw.lcd.density=420
hw.ramSize=3072
```

## Запуск эмулятора

```bash
export ANDROID_HOME=~/Android/Sdk
$ANDROID_HOME/emulator/emulator -avd ExpenseTracker_360 &
```

`&` запускает эмулятор в фоне — терминал остаётся свободным.

Проверить, что устройство подключилось:

```bash
$ANDROID_HOME/platform-tools/adb devices
```

Ожидаемый вывод: `emulator-5554 device` (номер порта может отличаться).

### Через Android Studio

**Device Manager** → выберите **`ExpenseTracker_360`** → кнопка запуска (▶).

## Установка и запуск приложения

Из корня репозитория:

```bash
./gradlew :app:installDebug
```

Открыть приложение на эмуляторе:

```bash
$ANDROID_HOME/platform-tools/adb shell am start -n com.olegbelyanin.expensetracker/.MainActivity
```

В Android Studio: **Run** на модуле `:app` (эмулятор должен быть уже запущен).

## Первый запуск на новом AVD

1. **Settings → System → Gestures → System navigation → Gesture navigation**
2. Для safe area (вырез): **Developer options → Simulate a display with a cutout → Punch hole**
3. При необходимости раскладки Gboard (EN/RU):

   ```bash
   ./scripts/configure-emulator-keyboards.sh
   ```

При первом запуске debug-сборки приложение импортирует seed и демо-расходы —
может появиться экран загрузки.

## Остановка эмулятора

Закрыть окно эмулятора или:

```bash
$ANDROID_HOME/platform-tools/adb -s emulator-5554 emu kill
```

Замените `emulator-5554` на id из `adb devices`, если порт другой.

## Для чего используется этот AVD

- ежедневная разработка UI под макет 360×800;
- проверка светлой/тёмной темы, safe area, режима полёта (F-08);
- SAF для экспорта и backup;
- инструментальные тесты Room/FTS и Compose UI (локально, не в CI).
