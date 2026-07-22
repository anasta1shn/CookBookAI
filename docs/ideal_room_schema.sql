-- Ideal Room / SQLite schema for CookBookAI.
-- This schema mirrors the main Supabase entities needed for offline work.

create table if not exists profiles (
    id text not null primary key,
    email text,
    fullName text,
    avatarUrl text,
    createdAt text,
    updatedAt text
);

create table if not exists foods (
    id text not null primary key,
    name text not null,
    category text not null,
    calories real not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0
);

create table if not exists recipes (
    id text not null primary key,
    userId text,
    title text not null,
    description text not null default '',
    imageUri text,
    category text not null default 'Без категории',
    cookingTime integer not null default 0,
    servings integer not null default 1,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    createdAt text,
    updatedAt integer not null
);

create table if not exists recipe_ingredients (
    id text not null primary key,
    recipeId text not null,
    foodId text,
    name text not null,
    weight real not null default 0,
    unit text not null default 'г',
    calories real not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    foreign key (recipeId) references recipes(id) on delete cascade,
    foreign key (foodId) references foods(id) on delete set null
);

create table if not exists favorites (
    userId text not null,
    recipeId text not null,
    createdAt integer not null,
    primary key (userId, recipeId),
    foreign key (recipeId) references recipes(id) on delete cascade
);

create table if not exists analysis_history (
    id text not null primary key,
    userId text,
    name text not null,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    imageUri text,
    weight integer not null default 0,
    confidence real not null default 0,
    aiSummary text not null default '',
    topPredictions text not null default '',
    date integer not null
);

create table if not exists analysis_components (
    id text not null primary key,
    analysisId text not null,
    foodId text,
    name text not null,
    weight integer not null default 0,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    confidence real not null default 0,
    foreign key (analysisId) references analysis_history(id) on delete cascade,
    foreign key (foodId) references foods(id) on delete set null
);

create table if not exists barcode_products (
    barcode text not null primary key,
    userId text,
    name text not null,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    weight integer not null default 100,
    createdAt text
);

create table if not exists analysis_corrections (
    id text not null primary key,
    userId text,
    imageHash text not null,
    modelPrediction text,
    correctedName text not null,
    createdAt integer not null
);

create unique index if not exists idx_analysis_corrections_user_hash
    on analysis_corrections(userId, imageHash);

create index if not exists idx_recipes_user_id on recipes(userId);
create index if not exists idx_recipe_ingredients_recipe_id on recipe_ingredients(recipeId);
create index if not exists idx_favorites_recipe_id on favorites(recipeId);
create index if not exists idx_analysis_history_user_id on analysis_history(userId);
create index if not exists idx_analysis_components_analysis_id on analysis_components(analysisId);
create index if not exists idx_barcode_products_user_id on barcode_products(userId);
create index if not exists idx_foods_name on foods(name);
