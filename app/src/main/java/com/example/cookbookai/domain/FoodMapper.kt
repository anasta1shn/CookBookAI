package com.example.cookbookai.domain

import com.example.cookbookai.network.model.Detection

class FoodMapper {

    private val classAliases =
        mapOf(
            "pasta" to "side_dish",
            "rice" to "side_dish",
            "potato" to "side_dish",
            "potatoes" to "side_dish",
            "buckwheat" to "side_dish",
            "chicken" to "meat",
            "beef" to "meat",
            "pork" to "meat",
            "sausage" to "meat",
            "fish" to "fish",
            "salmon" to "fish",
            "egg" to "egg",
            "omelet" to "egg",
            "omelette" to "egg",
            "tomato" to "vegetable",
            "cucumber" to "vegetable",
            "pepper" to "vegetable",
            "bell_pepper" to "vegetable",
            "carrot" to "vegetable",
            "cabbage" to "vegetable",
            "broccoli" to "vegetable",
            "cauliflower" to "vegetable",
            "corn" to "vegetable",
            "peas" to "vegetable",
            "beans" to "vegetable",
            "zucchini" to "vegetable",
            "eggplant" to "vegetable",
            "lettuce" to "vegetable",
            "greens" to "vegetable",
            "apple" to "fruit",
            "banana" to "fruit",
            "orange" to "fruit",
            "grape" to "fruit",
            "grapes" to "fruit",
            "pear" to "fruit",
            "kiwi" to "fruit",
            "strawberry" to "fruit",
            "berries" to "fruit",
            "watermelon" to "fruit",
            "melon" to "fruit",
            "pineapple" to "fruit",
            "mango" to "fruit",
            "peach" to "fruit",
            "plum" to "fruit",
            "mandarin" to "fruit",
            "pizza" to "fast_food",
            "burger" to "fast_food",
            "sandwich" to "fast_food",
            "cake" to "dessert",
            "ice_cream" to "dessert",
            "bread" to "bakery",
            "croissant" to "bakery",
            "coffee" to "drink",
            "tea" to "drink",
            "juice" to "drink"
        )

    fun mapDetections(
        detections: List<Detection>
    ): MappingResult {

        if (detections.isEmpty()) {

            return MappingResult(
                foodName = "Неизвестно"
            )
        }

        // ============================================
        // ФИЛЬТР ПО CONFIDENCE
        // ============================================

        val filteredDetections =
            detections.filter {
                it.confidence >= 0.35f
            }

        if (filteredDetections.isEmpty()) {

            return MappingResult(
                foodName = "Неизвестное блюдо"
            )
        }

        val classes =
            filteredDetections.map {
                normalizeClass(
                    it.`class`
                )
            }

        // ============================================
        // ПРИОРИТЕТНЫЕ КОМБИНАЦИИ
        // ============================================

        // 🍝 ГАРНИР + МЯСО
        if (
            classes.contains("side_dish") &&
            classes.contains("meat") &&
            classes.contains("vegetable")
        ) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Гречка + Свинина",
                    "Гречка + Курица",
                    "Рис + Курица",
                    "Рис + Свинина",
                    "Картофель + Свинина",
                    "Картофельное пюре + Котлета",
                    "Макароны с гуляшом",
                    "Рис с курицей",
                    "Гречка с котлетой",
                    "Картофельное пюре с котлетой",
                    "Курица с овощами"
                ),

                detectedClasses = listOf(
                    "side_dish",
                    "meat",
                    "vegetable"
                )
            )
        }

        if (
            classes.contains("side_dish") &&
            classes.contains("meat")
        ) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Гречка + Свинина",
                    "Гречка + Курица",
                    "Рис + Курица",
                    "Рис + Свинина",
                    "Картофель + Свинина",
                    "Картофельное пюре + Котлета",
                    "Макароны с котлетой",
                    "Рис с курицей",
                    "Гречка с курицей",
                    "Картофельное пюре с котлетой",
                    "Плов"
                ),

                detectedClasses = listOf(
                    "side_dish",
                    "meat"
                )
            )
        }

        // 🍳 ЯЙЦО + ОВОЩИ
        if (
            classes.contains("egg") &&
            classes.contains("vegetable")
        ) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Омлет с овощами",
                    "Яичница с овощами",
                    "Омлет с болгарским перцем"
                ),

                detectedClasses = listOf(
                    "egg"
                )
            )
        }

        // 🐟 РЫБА + ГАРНИР
        if (
            classes.contains("fish") &&
            classes.contains("side_dish")
        ) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Рыба с рисом",
                    "Рыба с картофелем",
                    "Рыба с овощами",
                    "Лосось с рисом",
                    "Минтай с картофелем"
                ),

                detectedClasses = listOf(
                    "fish"
                )
            )
        }

        // ============================================
        // СУПЫ
        // ============================================

        if (classes.contains("soup")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Борщ",
                    "Щи",
                    "Солянка",
                    "Окрошка",
                    "Куриный суп",
                    "Грибной суп",
                    "Уха",
                    "Свекольник",
                    "Суп-пюре",
                    "Гороховый суп",
                    "Рассольник"
                ),

                detectedClasses = listOf(
                    "soup"
                )
            )
        }

        // ============================================
        // САЛАТЫ
        // ============================================

        if (classes.contains("salad")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Цезарь",
                    "Оливье",
                    "Греческий салат",
                    "Винегрет",
                    "Селедка под шубой",
                    "Овощной салат",
                    "Крабовый салат",
                    "Мимоза",
                    "Салат с курицей"
                ),

                detectedClasses = listOf(
                    "salad"
                )
            )
        }

        // ============================================
        // ЯЙЦА
        // ============================================

        if (classes.contains("egg")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Омлет",
                    "Яичница",
                    "Вареное яйцо"
                ),

                detectedClasses = listOf(
                    "egg"
                )
            )
        }

        // ============================================
        // МЯСО
        // ============================================

        if (classes.contains("meat")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Курица",
                    "Куриная грудка",
                    "Котлета",
                    "Куриная котлета",
                    "Говядина",
                    "Свинина",
                    "Индейка",
                    "Гуляш",
                    "Сосиски"
                ),

                detectedClasses = listOf(
                    "meat"
                )
            )
        }

        // ============================================
        // РЫБА
        // ============================================

        if (classes.contains("fish")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Лосось",
                    "Тунец",
                    "Горбуша",
                    "Минтай",
                    "Селедка",
                    "Жареная рыба"
                ),

                detectedClasses = listOf(
                    "fish"
                )
            )
        }

        // ============================================
        // ГАРНИРЫ
        // ============================================

        if (classes.contains("side_dish")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Макароны",
                    "Рис",
                    "Гречка",
                    "Картофель",
                    "Картофельное пюре",
                    "Булгур"
                ),

                detectedClasses = listOf(
                    "side_dish"
                )
            )
        }

        // ============================================
        // ОВОЩИ
        // ============================================

        if (classes.contains("vegetable")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Овощная тарелка",
                    "Овощной салат",
                    "Болгарский перец",
                    "Помидоры",
                    "Огурцы",
                    "Морковь",
                    "Капуста",
                    "Брокколи",
                    "Кукуруза",
                    "Тушеные овощи",
                    "Овощи гриль"
                ),

                detectedClasses = listOf(
                    "vegetable"
                )
            )
        }

        // ============================================
        // ФРУКТЫ
        // ============================================

        if (classes.contains("fruit")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Фруктовая тарелка",
                    "Яблоко",
                    "Банан",
                    "Апельсин",
                    "Виноград",
                    "Груша",
                    "Киви",
                    "Клубника",
                    "Арбуз",
                    "Дыня",
                    "Манго"
                ),

                detectedClasses = listOf(
                    "fruit"
                )
            )
        }

        // ============================================
        // ДЕСЕРТЫ
        // ============================================

        if (classes.contains("dessert")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Торт",
                    "Пончик",
                    "Мороженое",
                    "Сырники",
                    "Творог",
                    "Пирожное"
                ),

                detectedClasses = listOf(
                    "dessert"
                )
            )
        }

        // ============================================
        // ВЫПЕЧКА
        // ============================================

        if (classes.contains("bakery")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Круассан",
                    "Пирог",
                    "Блины",
                    "Хлеб",
                    "Сэндвич"
                ),

                detectedClasses = listOf(
                    "bakery"
                )
            )
        }

        // ============================================
        // ФАСТФУД
        // ============================================

        if (classes.contains("fast_food")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Пицца",
                    "Бургер",
                    "Хот-дог",
                    "Шаурма",
                    "Сэндвич"
                ),

                detectedClasses = listOf(
                    "fast_food"
                )
            )
        }

        // ============================================
        // НАПИТКИ
        // ============================================

        if (classes.contains("drink")) {

            return MappingResult(

                needUserChoice = true,

                options = listOf(
                    "Чай",
                    "Кофе",
                    "Капучино",
                    "Латте",
                    "Газировка",
                    "Сок"
                ),

                detectedClasses = listOf(
                    "drink"
                )
            )
        }

        // ============================================
        // UNKNOWN
        // ============================================

        return MappingResult(
            foodName = "Неизвестное блюдо"
        )
    }

    private fun normalizeClass(
        value: String
    ): String {

        val normalized =
            value
                .lowercase()
                .replace("-", "_")
                .replace(" ", "_")
                .trim()

        return classAliases[normalized]
            ?: normalized
    }
}
