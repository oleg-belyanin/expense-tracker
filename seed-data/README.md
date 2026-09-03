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

## Формат CSV

```csv
name,location,category_code
Латте,Шоколадница,CAFE
```

## Воспроизведение seed-артефакта

После реализации `:seed-generator`:

```bash
./gradlew :seed-generator:generateSeed
```

Артефакт попадает в `app/src/main/assets/seed/`.

## Лицензия

Синтетические данные проекта, без персональной информации.
