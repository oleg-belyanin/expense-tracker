#!/usr/bin/env bash
# Создаёт AVD ExpenseTracker_360 (этап I1 плана работ).
set -euo pipefail

AVD_NAME="ExpenseTracker_360"
SYSTEM_IMAGE="system-images;android-34;google_apis;x86_64"
# Pixel 4 — punch-hole в рамке эмулятора; размер экрана задаём в config.ini.
BASE_DEVICE="pixel_4"

JAVA_HOME="${JAVA_HOME:-$HOME/.local/jdk-17}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export JAVA_HOME
export ANDROID_HOME
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager"

if [[ ! -x "$SDKMANAGER" ]]; then
  echo "Android SDK cmdline-tools не найдены: $SDKMANAGER" >&2
  exit 1
fi

if [[ ! -d "$ANDROID_HOME/system-images/android-34/google_apis/x86_64" ]]; then
  echo "Устанавливаю emulator и system image API 34..."
  yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" "emulator" "$SYSTEM_IMAGE"
fi

echo "Создаю AVD $AVD_NAME (база $BASE_DEVICE)..."
echo no | "$AVDMANAGER" create avd \
  -n "$AVD_NAME" \
  -k "$SYSTEM_IMAGE" \
  -d "$BASE_DEVICE" \
  -f

CONFIG="$HOME/.android/avd/${AVD_NAME}.avd/config.ini"

set_ini() {
  local key="$1"
  local value="$2"
  if grep -q "^${key}=" "$CONFIG"; then
    sed -i "s|^${key}=.*|${key}=${value}|" "$CONFIG"
  else
    echo "${key}=${value}" >> "$CONFIG"
  fi
}

# 360×800 dp @ 420 dpi = 945×2100 px (как кадры Figma)
set_ini hw.lcd.width 945
set_ini hw.lcd.height 2100
set_ini hw.lcd.density 420
set_ini hw.ramSize 3072
set_ini hw.mainKeys no
set_ini showDeviceFrame yes

echo
echo "AVD $AVD_NAME готов."
echo "Запуск: emulator -avd $AVD_NAME"
echo "После загрузки — две раскладки Gboard (EN/RU):"
echo "  $(dirname "$0")/configure-emulator-keyboards.sh"
echo "Первый запуск: Settings → System → Gestures → System navigation → Gesture navigation"
