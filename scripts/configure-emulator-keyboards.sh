#!/usr/bin/env bash
# Включает системные языки en-US и ru-RU, чтобы Gboard дал две раскладки.
set -euo pipefail

DUMP="/tmp/et-uidump.xml"

wait_for_device() {
  adb wait-for-device
  for _ in $(seq 1 40); do
    if [[ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
      return 0
    fi
    sleep 2
  done
  echo "Эмулятор не загрузился" >&2
  exit 1
}

locales() {
  adb shell settings get system system_locales 2>/dev/null | tr -d '\r'
}

has_ru_en() {
  local current
  current="$(locales)"
  [[ "$current" == *ru-RU* && "$current" == *en-US* ]]
}

dump_ui() {
  adb shell uiautomator dump /sdcard/uidump.xml >/dev/null
  adb exec-out cat /sdcard/uidump.xml > "$DUMP"
}

tap_label() {
  local label="$1"
  dump_ui
  python3 - "$DUMP" "$label" <<'PY'
import sys
from xml.etree import ElementTree as ET

path, label = sys.argv[1], sys.argv[2]
root = ET.parse(path).getroot()
nodes = list(root.iter("node"))
parent = {c: n for n in nodes for c in n}

def bounds(node):
    raw = node.attrib.get("bounds", "")
    nums = [int(x) for x in raw.replace("][", ",").replace("[", "").replace("]", "").split(",") if x]
    if len(nums) != 4:
        raise SystemExit(f"no bounds for {label!r}")
    return (nums[0] + nums[2]) // 2, (nums[1] + nums[3]) // 2

for node in nodes:
    if node.attrib.get("text") == label or node.attrib.get("content-desc") == label:
        current = node
        while current is not None and current.attrib.get("clickable") != "true":
            current = parent.get(current)
        target = current if current is not None else node
        x, y = bounds(target)
        print(f"{x} {y}")
        raise SystemExit(0)
raise SystemExit(f"UI node not found: {label}")
PY
}

tap() {
  local point
  point="$(tap_label "$1")"
  adb shell input tap $point
  sleep 1
}

add_russian() {
  adb shell am start -a android.settings.LOCALE_SETTINGS >/dev/null
  sleep 2
  tap "Add a language"
  sleep 1
  tap "Search"
  sleep 1
  adb shell input text 'Russian'
  sleep 1
  tap "Русский"
  sleep 1
  tap "Россия"
  sleep 2
}

wait_for_device
if has_ru_en; then
  echo "Раскладки уже есть: $(locales)"
  exit 0
fi

echo "Добавляю ru-RU к системным языкам (сейчас: $(locales))..."
add_russian
if ! has_ru_en; then
  echo "Не удалось добавить ru-RU. Сейчас: $(locales)" >&2
  exit 1
fi
echo "Готово: $(locales)"
echo "В Gboard переключение EN/RU — пробел или иконка языка."
