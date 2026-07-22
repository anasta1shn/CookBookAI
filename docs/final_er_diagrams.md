# Итоговые ER-диаграммы CookBookAI

Эти диаграммы объединяют текущую реализацию проекта и идеальную нормализованную структуру БД для диплома.  
В текущем приложении часть данных уже реализована проще: например, избранное хранится полем `isFavorite`, ингредиенты рецепта хранятся строкой, а компоненты анализа сериализуются в результате. В итоговой дипломной схеме эти данные вынесены в отдельные таблицы, чтобы показать правильную структуру связей.

## Room

```mermaid
erDiagram
    PROFILES {
        string id PK
        string email
        string fullName
        string avatarUrl
        string createdAt
        string updatedAt
    }

    FOODS {
        string id PK
        string name
        string category
        float calories
        float proteins
        float fats
        float carbs
    }

    RECIPES {
        string id PK
        string userId FK
        string title
        string description
        string imageUri
        string category
        int cookingTime
        int servings
        int calories
        float proteins
        float fats
        float carbs
        string createdAt
        long updatedAt
    }

    RECIPE_INGREDIENTS {
        string id PK
        string recipeId FK
        string foodId FK
        string name
        float weight
        string unit
        float calories
        float proteins
        float fats
        float carbs
    }

    FAVORITES {
        string userId PK,FK
        string recipeId PK,FK
        long createdAt
    }

    ANALYSIS_HISTORY {
        string id PK
        string userId FK
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

    ANALYSIS_COMPONENTS {
        string id PK
        string analysisId FK
        string foodId FK
        string name
        int weight
        int calories
        float proteins
        float fats
        float carbs
        float confidence
    }

    BARCODE_PRODUCTS {
        string barcode PK
        string userId FK
        string name
        int calories
        float proteins
        float fats
        float carbs
        int weight
        string createdAt
    }

    ANALYSIS_CORRECTIONS {
        string id PK
        string userId FK
        string imageHash
        string modelPrediction
        string correctedName
        long createdAt
    }

    PROFILES ||--o{ RECIPES : "создает"
    PROFILES ||--o{ FAVORITES : "имеет"
    PROFILES ||--o{ ANALYSIS_HISTORY : "выполняет"
    PROFILES ||--o{ BARCODE_PRODUCTS : "добавляет"
    PROFILES ||--o{ ANALYSIS_CORRECTIONS : "исправляет"

    RECIPES ||--o{ RECIPE_INGREDIENTS : "содержит"
    RECIPES ||--o{ FAVORITES : "добавлен"
    FOODS ||--o{ RECIPE_INGREDIENTS : "используется"

    ANALYSIS_HISTORY ||--o{ ANALYSIS_COMPONENTS : "состоит из"
    FOODS ||--o{ ANALYSIS_COMPONENTS : "соответствует"
```

## Supabase

```mermaid
erDiagram
    AUTH_USERS {
        uuid id PK
        string email
        jsonb user_metadata
        timestamptz created_at
    }

    PROFILES {
        uuid id PK,FK
        string email
        string full_name
        string avatar_url
        timestamptz created_at
        timestamptz updated_at
    }

    FOODS {
        uuid id PK
        string name
        string category
        float calories
        float proteins
        float fats
        float carbs
        timestamptz created_at
    }

    RECIPES {
        uuid id PK
        uuid user_id FK
        string title
        string description
        string image_url
        string category
        int cooking_time
        int servings
        int calories
        float proteins
        float fats
        float carbs
        timestamptz created_at
        timestamptz updated_at
    }

    RECIPE_INGREDIENTS {
        uuid id PK
        uuid recipe_id FK
        uuid food_id FK
        string name
        float weight
        string unit
        float calories
        float proteins
        float fats
        float carbs
    }

    FAVORITES {
        uuid user_id PK,FK
        uuid recipe_id PK,FK
        timestamptz created_at
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

    ANALYSIS_COMPONENTS {
        uuid id PK
        uuid analysis_id FK
        uuid food_id FK
        string name
        int weight
        int calories
        float proteins
        float fats
        float carbs
        float confidence
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

    ANALYSIS_CORRECTIONS {
        uuid id PK
        uuid user_id FK
        string image_hash
        string model_prediction
        string corrected_name
        timestamptz created_at
    }

    STORAGE_OBJECTS {
        uuid id PK
        string bucket_id
        string name
        uuid owner FK
        timestamptz created_at
    }

    AUTH_USERS ||--|| PROFILES : "имеет профиль"
    AUTH_USERS ||--o{ RECIPES : "создает"
    AUTH_USERS ||--o{ FAVORITES : "добавляет"
    AUTH_USERS ||--o{ ANALYSIS_RESULTS : "выполняет"
    AUTH_USERS ||--o{ BARCODE_PRODUCTS : "добавляет"
    AUTH_USERS ||--o{ ANALYSIS_CORRECTIONS : "исправляет"
    AUTH_USERS ||--o{ STORAGE_OBJECTS : "загружает"

    RECIPES ||--o{ RECIPE_INGREDIENTS : "содержит"
    RECIPES ||--o{ FAVORITES : "сохранен"
    FOODS ||--o{ RECIPE_INGREDIENTS : "используется"

    ANALYSIS_RESULTS ||--o{ ANALYSIS_COMPONENTS : "состоит из"
    FOODS ||--o{ ANALYSIS_COMPONENTS : "соответствует"

    RECIPES }o..o| STORAGE_OBJECTS : "image_url"
    ANALYSIS_RESULTS }o..o| STORAGE_OBJECTS : "image_url"
    PROFILES }o..o| STORAGE_OBJECTS : "avatar_url"
```
