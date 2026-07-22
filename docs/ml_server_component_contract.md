# ML Server Component Contract

Цель: для составных тарелок сервер должен сначала найти компоненты на фото, затем классифицировать каждый компонент, а не возвращать только один общий класс.

## Рекомендуемый ответ `/predict`

```json
{
  "mode": "multi_food",
  "dish": "Гречка + Свинина",
  "confidence": 0.86,
  "components": [
    {
      "name": "Гречка",
      "class_name": "buckwheat",
      "confidence": 0.88
    },
    {
      "name": "Свинина",
      "class_name": "pork",
      "confidence": 0.84
    }
  ],
  "detections": [
    {
      "class": "buckwheat",
      "confidence": 0.88
    },
    {
      "class": "pork",
      "confidence": 0.84
    }
  ],
  "top_predictions": [
    {
      "name": "Гречка + Свинина",
      "confidence": 0.86
    },
    {
      "name": "Гречка + Курица",
      "confidence": 0.08
    },
    {
      "name": "Свинина",
      "confidence": 0.06
    }
  ]
}
```

## Минимальный рабочий ответ

Android уже сможет собрать составное блюдо, если сервер вернет хотя бы это:

```json
{
  "mode": "multi_food",
  "detections": [
    {
      "class": "buckwheat",
      "confidence": 0.88
    },
    {
      "class": "pork",
      "confidence": 0.84
    }
  ]
}
```

## Важное ограничение

Если сервер возвращает только:

```json
{
  "dish": "Свинина",
  "confidence": 0.91
}
```

приложение не может узнать, что на фото еще есть гречка. Для автоматической работы составных блюд сервер должен вернуть минимум два компонента.

## Классы, которые Android сейчас понимает

Гарниры:

- `buckwheat`, `grechka`, `гречка`
- `rice`, `рис`
- `pasta`, `macaroni`, `макароны`, `паста`
- `potato`, `potatoes`, `картофель`, `картошка`
- `mashed_potato`, `puree`, `пюре`, `картофельное_пюре`
- `bulgur`, `булгур`

Мясо и рыба:

- `chicken`, `курица`, `chicken_breast`, `куриная_грудка`
- `pork`, `свинина`
- `beef`, `говядина`
- `turkey`, `индейка`
- `cutlet`, `котлета`
- `fish`, `fried_fish`, `рыба`, `жареная_рыба`
- `salmon`, `лосось`
- `pollock`, `минтай`
