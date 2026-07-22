# ER-диаграммы CookBookAI

## Room, локальная база данных

В локальной базе Room используются три таблицы. В коде приложения внешние ключи между ними не объявлены: рецепты, история ИИ-анализа и продукты по штрихкоду хранятся независимо.

```mermaid
erDiagram
    RECIPES {
        string id PK
        string title
        string description
        string ingredients
        int calories
        int proteins
        int fats
        int carbs
        string imageUri
        boolean isFavorite
        string category
        int cookingTime
        string createdAt
        long updatedAt
    }

    ANALYSIS_HISTORY {
        string id PK
        string name
        int calories
        float proteins
        float fats
        float carbs
        string imageUri
        int weight
        float confidence
        string aiSummary
        string topPredictions
        long date
    }

    BARCODE_PRODUCTS {
        string barcode PK
        string name
        int calories
        float proteins
        float fats
        float carbs
        int weight
        string userId
        string createdAt
    }
```

## Supabase, удаленная база данных и Storage

В Supabase данные пользователя связаны с таблицами `analysis_results` и `barcode_products` через поле `user_id`. Рецепты в текущей реализации приложения не содержат `user_id`, поэтому строгой связи `auth.users -> recipes` в коде нет. Изображения рецептов загружаются в Supabase Storage bucket `recipe-images`, а в таблице `recipes` хранится публичная ссылка в поле `imageUri`.

```mermaid
erDiagram
    AUTH_USERS {
        uuid id PK
        string email
        jsonb user_metadata
        timestamptz created_at
    }

    RECIPES {
        string id PK
        string title
        string description
        string ingredients
        int calories
        int proteins
        int fats
        int carbs
        string imageUri
        boolean isFavorite
        string category
        int cookingTime
        timestamptz created_at
        bigint updatedAt
    }

    ANALYSIS_RESULTS {
        uuid id PK
        uuid user_id FK
        string name
        int calories
        float proteins
        float fats
        float carbs
        string image_url
        int weight
        float confidence
        string ai_summary
        string top_predictions
        timestamptz created_at
    }

    BARCODE_PRODUCTS {
        string barcode PK
        uuid user_id FK
        string name
        int calories
        float proteins
        float fats
        float carbs
        int weight
        timestamptz created_at
    }

    FOODS {
        uuid id PK
        string name
        string category
        float calories
        float proteins
        float fats
        float carbs
    }

    STORAGE_OBJECTS {
        uuid id PK
        string bucket_id
        string name
        uuid owner
        timestamptz created_at
    }

    AUTH_USERS ||--o{ ANALYSIS_RESULTS : "owns user_id"
    AUTH_USERS ||--o{ BARCODE_PRODUCTS : "owns user_id"
    AUTH_USERS ||--o{ STORAGE_OBJECTS : "uploads owner"
    RECIPES }o..o| STORAGE_OBJECTS : "imageUri public URL"
```
