package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.BuildConfig
import com.example.stylish_android_application.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class WeatherViewModel : ViewModel() {

    private val _weatherPosts = MutableLiveData<List<Post>>()
    val weatherPosts: LiveData<List<Post>> = _weatherPosts

    private val _cityName = MutableLiveData<String>()
    val cityName: LiveData<String> = _cityName

    private val _temperature = MutableLiveData<String>()
    val temperature: LiveData<String> = _temperature

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    fun fetchWeatherByCoordinates(lat: Double, lon: Double, geocoderCityName: String) {
        val apiKey = BuildConfig.WEATHER_API_KEY
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
        executeWeatherApi(url, geocoderCityName)
    }

    fun fetchWeatherAndData(cityName: String) {
        val apiKey = BuildConfig.WEATHER_API_KEY
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$cityName&appid=$apiKey&units=metric"
        executeWeatherApi(url, cityName)
    }

    private fun executeWeatherApi(url: String, cityOverride: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)

                val temp = jsonObject.getJSONObject("main").getDouble("temp").toInt()
                val apiCityName = jsonObject.getString("name")
                val finalCityName = if (cityOverride.isNotEmpty()) cityOverride else apiCityName

                val category = when {
                    temp < 15 -> "Cold"
                    temp in 15..25 -> "Warm"
                    else -> "Hot"
                }

                withContext(Dispatchers.Main) {
                    _cityName.value = finalCityName.uppercase()
                    _temperature.value = "$temp°"
                }

                loadPostsFromFirestore(category)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _errorMessage.value = "Weather Error: Unable to fetch data"
                }
            }
        }
    }

    private fun loadPostsFromFirestore(category: String) {
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("weatherCategory", category)
            .get()
            .addOnSuccessListener { result ->
                val postsList = mutableListOf<Post>()
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    postsList.add(post)
                }
                _weatherPosts.value = postsList
            }
            .addOnFailureListener {
                _errorMessage.value = "Failed to load posts"
            }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}