# Демо-расходы (не seed)

Это набор для списка и аналитики. Его нельзя путать с [`seed-data/`](../seed-data/):

| | `seed-data/` | `demo-data/` |
|---|---|---|
| Назначение | cold start категоризации | UI, скриншоты, NFR-2 |
| Поля | name, location, category_code | сумма, дата и остальные поля F-09 |
| Поставка | assets `seed/` в любом APK | только debug-assets, 300 строк |

## UI-набор (закоммичен)

[`expenses-ui.csv`](./expenses-ui.csv) — 300 строк. Имена и места скопированы
из `seed-data/raw/train.csv`, суммы и даты добавлены генератором
(`:core:domain` + `:seed-generator:generateDemo`). Якорь дат — 2026-09-04.

```bash
./gradlew :seed-generator:generateDemo
```

Пишет этот файл и копию в `app/src/debug/assets/demo/expenses.csv`.
`./gradlew check` сверяет, что оба совпадают с генерацией.

## NFR-2 (локально, не коммитить)

```bash
./gradlew :seed-generator:generateNfrDemo
```

Пишет `local/expenses-nfr-5000.csv` (~5 000 строк). Каталог в `.gitignore`.
