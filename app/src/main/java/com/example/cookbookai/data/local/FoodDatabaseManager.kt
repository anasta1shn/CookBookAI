package com.example.cookbookai.data.local

import android.content.Context
import com.example.cookbookai.data.model.FoodNutrition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FoodDatabaseManager {

    private const val PREFS_NAME =
        "custom_food_database"

    private const val CUSTOM_FOODS_KEY =
        "custom_foods"

    private var foods: List<FoodNutrition> = emptyList()

    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    private val aliases =
        mapOf(
            "паста" to "Макароны",
            "спагетти" to "Макароны",
            "рожки" to "Макароны",
            "пюре" to "Картофельное пюре",
            "картошка" to "Картофель",
            "картофель отварной" to "Картофель",
            "картофель вареный" to "Картофель вареный",
            "вареная картошка" to "Картофель вареный",
            "курица гриль" to "Курица",
            "куриная грудка" to "Курица",
            "куриное филе" to "Курица",
            "говядина тушеная" to "Говядина",
            "свинина тушеная" to "Свинина",
            "рыба" to "Жареная рыба",
            "красная рыба" to "Лосось",
            "омлет с овощами" to "Омлет",
            "яичница с овощами" to "Яичница",
            "вареное яйцо" to "Яйцо",
            "цезарь с курицей" to "Цезарь",
            "греческий" to "Греческий салат",
            "овощи" to "Тушеные овощи",
            "томат" to "Помидоры",
            "помидор" to "Помидоры",
            "огурец" to "Огурцы",
            "огурец соленый" to "Соленые огурцы",
            "соленый огурец" to "Соленые огурцы",
            "соленые огурцы" to "Соленые огурцы",
            "маринованный огурец" to "Маринованные огурцы",
            "лук" to "Лук",
            "лук репчатый" to "Лук",
            "красный лук" to "Красный лук",
            "зеленый лук" to "Зеленый лук",
            "сладкий перец" to "Болгарский перец",
            "перец" to "Болгарский перец",
            "вареная морковь" to "Морковь вареная",
            "морковь вареная" to "Морковь вареная",
            "вареная свекла" to "Свекла вареная",
            "свекла вареная" to "Свекла вареная",
            "горошек" to "Консервированный горошек",
            "зеленый горошек" to "Консервированный горошек",
            "кукуруза консервированная" to "Консервированная кукуруза",
            "суп" to "Куриный суп",
            "масло" to "Подсолнечное масло",
            "масло подсолнечное" to "Подсолнечное масло",
            "масло растительное" to "Растительное масло",
            "масло оливковое" to "Оливковое масло",
            "сметана" to "Сметана 15%",
            "майонез" to "Майонез",
            "йогурт" to "Натуральный йогурт",
            "йогуртовая заправка" to "Йогуртовая заправка",
            "соевый соус" to "Соевый соус",
            "лимонный сок" to "Лимонный сок",
            "соус цезарь" to "Соус Цезарь",
            "цезарь соус" to "Соус Цезарь",
            "бальзамик" to "Бальзамический уксус",
            "уксус бальзамический" to "Бальзамический уксус",
            "кетчуп" to "Кетчуп",
            "горчица" to "Горчица",
            "чесночный соус" to "Чесночный соус",
            "томатная паста" to "Томатная паста",
            "сухари" to "Сухарики",
            "сухарики" to "Сухарики",
            "пармезан" to "Пармезан",
            "фета" to "Фета",
            "брынза" to "Брынза",
            "маслины" to "Маслины",
            "оливки" to "Оливки",
            "крабовые палочки" to "Крабовые палочки",
            "краб палочки" to "Крабовые палочки",
            "тунец консервированный" to "Тунец консервированный",
            "салат айсберг" to "Салат айсберг",
            "айсберг" to "Салат айсберг",
            "листья салата" to "Листья салата",
            "пекинская капуста" to "Пекинская капуста",
            "куриный бульон" to "Куриный бульон",
            "говяжий бульон" to "Говяжий бульон",
            "рис отварной" to "Рис отварной",
            "рис для суши" to "Рис для суши",
            "нори" to "Нори",
            "лосось слабосоленый" to "Лосось слабосоленый",
            "творожный сыр" to "Творожный сыр",
            "креветки" to "Креветки",
            "киноа" to "Киноа",
            "нут" to "Нут",
            "хумус" to "Хумус",
            "тахини" to "Тахини",
            "кунжут" to "Семена кунжута",
            "семечки" to "Семечки подсолнечные",
            "пирожное" to "Торт",
            "кекс" to "Пирог",
            "хот дог" to "Хот-дог",
            "hot dog" to "Хот-дог",
            "burger" to "Бургер",
            "hamburger" to "Бургер",
            "pizza" to "Пицца",
            "pasta" to "Макароны",
            "rice" to "Рис",
            "buckwheat" to "Гречка",
            "chicken" to "Курица",
            "beef" to "Говядина",
            "pork" to "Свинина",
            "fish" to "Жареная рыба",
            "salmon" to "Лосось",
            "egg" to "Яйцо",
            "omelette" to "Омлет",
            "soup" to "Куриный суп",
            "salad" to "Овощной салат",
            "apple" to "Яблоко",
            "banana" to "Банан",
            "orange" to "Апельсин",
            "grape" to "Виноград",
            "grapes" to "Виноград",
            "pear" to "Груша",
            "kiwi" to "Киви",
            "strawberry" to "Клубника",
            "berries" to "Черника",
            "watermelon" to "Арбуз",
            "melon" to "Дыня",
            "pineapple" to "Ананас",
            "mango" to "Манго",
            "peach" to "Персик",
            "plum" to "Слива",
            "mandarin" to "Мандарин",
            "tomato" to "Помидоры",
            "cucumber" to "Огурцы",
            "pepper" to "Болгарский перец",
            "bellpepper" to "Болгарский перец",
            "carrot" to "Морковь",
            "boiledcarrot" to "Морковь вареная",
            "onion" to "Лук",
            "cabbage" to "Капуста",
            "broccoli" to "Брокколи",
            "cauliflower" to "Цветная капуста",
            "corn" to "Кукуруза",
            "peas" to "Горошек",
            "beans" to "Фасоль",
            "lettuce" to "Овощной салат",
            "oil" to "Подсолнечное масло",
            "sunfloweroil" to "Подсолнечное масло",
            "oliveoil" to "Оливковое масло",
            "sourcream" to "Сметана 15%",
            "mayonnaise" to "Майонез",
            "yogurt" to "Натуральный йогурт",
            "mustard" to "Горчица",
            "ketchup" to "Кетчуп",
            "caesarsauce" to "Соус Цезарь",
            "croutons" to "Сухарики",
            "parmesan" to "Пармезан",
            "feta" to "Фета",
            "olives" to "Оливки",
            "nori" to "Нори",
            "shrimp" to "Креветки",
            "quinoa" to "Киноа",
            "chickpeas" to "Нут",
            "hummus" to "Хумус",
            "coffee" to "Кофе",
            "tea" to "Чай"
        )

    fun loadDatabase(context: Context) {

        if (foods.isNotEmpty()) return

        val jsonString = context.assets
            .open("food_database.json")
            .bufferedReader()
            .use { it.readText() }

        val assetFoods: List<FoodNutrition> =
            json.decodeFromString(jsonString)

        foods =
            mergeFoods(
                assetFoods,
                loadCustomFoods(context)
            )
    }

    fun addCustomFood(
        context: Context,
        food: FoodNutrition
    ) {

        val customFoods =
            loadCustomFoods(context)

        val updatedCustomFoods =
            mergeFoods(
                customFoods,
                listOf(food)
            )

        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
            .edit()
            .putString(
                CUSTOM_FOODS_KEY,
                json.encodeToString(updatedCustomFoods)
            )
            .apply()

        foods =
            mergeFoods(
                foods,
                listOf(food)
            )
    }

    fun findFood(name: String): FoodNutrition? {

        val normalizedName =
            normalizeName(
                name
            )

        val aliasName =
            aliases.entries
                .firstOrNull {
                    normalizeName(it.key) == normalizedName
                }
                ?.value

        if (aliasName != null) {

            findFoodByNormalizedName(
                normalizeName(aliasName)
            )?.let {
                return it
            }
        }

        findFoodByNormalizedName(
            normalizedName
        )?.let {
            return it
        }

        if (looksLikeCompositeName(name)) {
            return null
        }

        val compactAlias =
            aliases.entries
                .firstOrNull {
                    val aliasKey =
                        normalizeName(it.key)

                    normalizedName.contains(aliasKey) ||
                            aliasKey.contains(normalizedName)
                }
                ?.value

        if (compactAlias != null) {

            findFoodByNormalizedName(
                normalizeName(compactAlias)
            )?.let {
                return it
            }
        }

        return null
    }

    private fun findFoodByNormalizedName(
        normalizedName: String
    ): FoodNutrition? {

        return foods.find {
            normalizeName(it.name) == normalizedName
        } ?: if (looksLikeCompositeNormalizedName(normalizedName)) {
            null
        } else {
            foods.find {
                val foodName =
                    normalizeName(it.name)

                foodName.contains(normalizedName) ||
                        normalizedName.contains(foodName)
            }
        }
    }

    fun getAllFoods(): List<FoodNutrition> {
        return foods
    }

    private fun loadCustomFoods(
        context: Context
    ): List<FoodNutrition> {

        val rawJson =
            context.getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
                .getString(
                    CUSTOM_FOODS_KEY,
                    null
                )

        return rawJson
            ?.let {
                runCatching {
                    json.decodeFromString<List<FoodNutrition>>(
                        it
                    )
                }.getOrNull()
            }
            ?: emptyList()
    }

    private fun mergeFoods(
        first: List<FoodNutrition>,
        second: List<FoodNutrition>
    ): List<FoodNutrition> {

        return (first + second)
            .associateBy {
                normalizeName(
                    it.name
                )
            }
            .values
            .toList()
    }

    private fun normalizeName(
        value: String
    ): String {

        return value
            .lowercase()
            .replace("ё", "е")
            .replace(Regex("[^а-яa-z0-9]+"), "")
            .trim()
    }

    private fun looksLikeCompositeName(
        value: String
    ): Boolean {

        val normalized =
            " ${value.lowercase().replace("ё", "е")} "

        return normalized.contains("+") ||
                normalized.contains(" с ") ||
                normalized.contains(" со ") ||
                normalized.contains(" и ") ||
                normalized.contains(" with ")
    }

    private fun looksLikeCompositeNormalizedName(
        value: String
    ): Boolean {

        return value.contains(" ") &&
                (
                        value.contains(" с ") ||
                                value.contains(" со ") ||
                                value.contains(" и ") ||
                                value.contains(" with ")
                        )
    }
}
