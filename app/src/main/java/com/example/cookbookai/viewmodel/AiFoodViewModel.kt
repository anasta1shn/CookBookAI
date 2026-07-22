package com.example.cookbookai.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cookbookai.data.model.AnalysisResult
import com.example.cookbookai.data.repository.FoodRepository
import kotlinx.coroutines.launch

class AiFoodViewModel(
    private val repository: FoodRepository
) : ViewModel() {

    val foods = MutableLiveData<List<AnalysisResult>>()

    fun searchFood(name: String) {

        viewModelScope.launch {

            val result = repository.searchFood(name)

            foods.postValue(result)
        }
    }
}