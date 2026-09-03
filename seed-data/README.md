# Seed-датасет для cold start категоризации

Размеченный синтетический датасет для офлайн-генерации seed-счётчиков.

**Полный план:** [`docs/seed-dataset-plan.md`](../docs/seed-dataset-plan.md)

## Объём

| Набор | Строк | На категорию |
|---|---:|---:|
| **Итого** | 1 000 | 100 |
| train (`raw/train.csv`) | 800 | 80 |
| validation (`raw/validation.csv`) | 200 | 20 |

10 категорий — см. [`categories.yaml`](./categories.yaml).

Разбиение stratified, без пересечения по `normalized_name` (как в `TextNormalizer` без стеммера).
Пустое `location`: 37,5 % строк (цель плана 30–40 %).

## Формат CSV

```csv
name,location,category_code
Латте,Шоколадница,CAFE
```

## Воспроизведение raw CSV

```bash
python3 seed-data/scripts/generate_raw_dataset.py
```

Скрипт перезаписывает `raw/train.csv` и `raw/validation.csv`, проверяет объём,
golden-строки, edge-case validation и отсутствие коллизий нормализации.

## Golden и edge-case

В **train** (§5.2 плана): Латте / Шоколадница, Кетостерил / Столичка на Чкалова,
Бензин / Лукойл, Хлеб, Непонятная покупка, Врач / Поликлиника, Стоматолог / Стоматология.

В **validation** (конфликт name vs location): Аспирин / Пятёрочка → HEALTH;
Витамины аптечные / Магнит → HEALTH; Губки для посуды / Пятёрочка → HOME.

Однозначные места (Шоколадница, Столичка, Лукойл) и неоднозначные супермаркеты
(Пятёрочка, Магнит) намешаны по правилам плана.

## Воспроизведение seed-артефакта

```bash
./gradlew :seed-generator:generateSeed
```

Пишет `app/src/main/assets/seed/`, фиксирует параметры в
`categorization-config.json` и отчёт в `reports/seed-v1-validation.json`.

CI сверяет, что закоммиченные файлы совпадают с повторной генерацией.

Зафиксированные параметры seed v1: `MIN_SEED_SUPPORT=1`,
`MIN_SEED_PROBABILITY=0.55`, `MAX_SEED_STRENGTH=10`, равные веса имени и места,
`LAPLACE_ALPHA=0.1`. Validation: top-1 85 %, fallback 10 %, top-3 89 %.

## Лицензия

Синтетические данные проекта, без персональной информации.
