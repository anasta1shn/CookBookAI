-- Ideal Supabase schema for CookBookAI
-- auth.users is created and managed by Supabase Auth.

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    email text,
    full_name text,
    avatar_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.foods (
    id uuid primary key default gen_random_uuid(),
    name text not null,
    category text not null,
    calories real not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    created_at timestamptz not null default now(),
    unique (name, category)
);

create table if not exists public.recipes (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    description text not null default '',
    image_url text,
    category text not null default 'Без категории',
    cooking_time integer not null default 0,
    servings integer not null default 1,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.recipe_ingredients (
    id uuid primary key default gen_random_uuid(),
    recipe_id uuid not null references public.recipes(id) on delete cascade,
    food_id uuid references public.foods(id) on delete set null,
    name text not null,
    weight real not null default 0,
    unit text not null default 'г',
    calories real not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0
);

create table if not exists public.favorites (
    user_id uuid not null references auth.users(id) on delete cascade,
    recipe_id uuid not null references public.recipes(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, recipe_id)
);

create table if not exists public.analysis_results (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    image_url text,
    weight integer not null default 0,
    confidence real not null default 0,
    ai_summary text not null default '',
    top_predictions text not null default '',
    created_at timestamptz not null default now()
);

create table if not exists public.analysis_components (
    id uuid primary key default gen_random_uuid(),
    analysis_id uuid not null references public.analysis_results(id) on delete cascade,
    food_id uuid references public.foods(id) on delete set null,
    name text not null,
    weight integer not null default 0,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    confidence real not null default 0
);

create table if not exists public.barcode_products (
    barcode text primary key,
    user_id uuid references auth.users(id) on delete set null,
    name text not null,
    calories integer not null default 0,
    proteins real not null default 0,
    fats real not null default 0,
    carbs real not null default 0,
    weight integer not null default 100,
    created_at timestamptz not null default now()
);

create table if not exists public.analysis_corrections (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    image_hash text not null,
    model_prediction text,
    corrected_name text not null,
    created_at timestamptz not null default now(),
    unique (user_id, image_hash)
);

create index if not exists idx_recipes_user_id on public.recipes(user_id);
create index if not exists idx_recipe_ingredients_recipe_id on public.recipe_ingredients(recipe_id);
create index if not exists idx_favorites_recipe_id on public.favorites(recipe_id);
create index if not exists idx_analysis_results_user_id on public.analysis_results(user_id);
create index if not exists idx_analysis_components_analysis_id on public.analysis_components(analysis_id);
create index if not exists idx_barcode_products_user_id on public.barcode_products(user_id);
create index if not exists idx_analysis_corrections_user_id on public.analysis_corrections(user_id);
create index if not exists idx_foods_name on public.foods(name);
