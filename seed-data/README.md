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

После реализации `:seed-generator`:

```bash
./gradlew :seed-generator:generateSeed
```

Артефакт попадает в `app/src/main/assets/seed/`.

## Лицензия

Синтетические данные проекта, без персональной информации.
