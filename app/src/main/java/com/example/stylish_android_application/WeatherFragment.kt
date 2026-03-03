package com.example.stylish_android_application

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentWeatherBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import android.location.Geocoder
import java.util.Locale

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfileAdapter
    private val postsList = mutableListOf<Post>()

    // המשתנה שאחראי על שליפת המיקום
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // מנגנון בקשת ההרשאה הקופצת מהמשתמש
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // המשתמש אישר! נביא את המיקום שלו
            getCurrentLocation()
        } else {
            // המשתמש סירב, נשתמש בחיפה כברירת מחדל
            Toast.makeText(context, "Location permission denied. Using default city.", Toast.LENGTH_SHORT).show()
            fetchWeatherAndData("Haifa")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // אתחול שירות המיקום
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // בודקים אם כבר יש לנו הרשאה
        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        binding.rvWeatherPosts.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfileAdapter(postsList, onPostClick = { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }, onPostLongClick = {})
        binding.rvWeatherPosts.adapter = adapter
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // כבר נתנו לנו הרשאה בעבר
                getCurrentLocation()
            }
            else -> {
                // פעם ראשונה - נקפיץ את חלונית ההרשאה
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // פה קורה הקסם: אנחנו מוציאים את שם העיר האמיתי מגוגל ולא ממזג האוויר!
                var realCityName = ""
                try {
                    val geocoder = Geocoder(requireContext(), Locale.ENGLISH) // אנגלית כדי שיוצג יפה במסך
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        // locality זה שם העיר המרכזית (למשל: Haifa, Tel Aviv)
                        realCityName = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // מעבירים את הקואורדינטות *וגם* את שם העיר שמצאנו
                fetchWeatherByCoordinates(location.latitude, location.longitude, realCityName)
            } else {
                fetchWeatherAndData("Haifa")
            }
        }
    }

    // --- הבאת מזג אוויר לפי קואורדינטות (GPS) ---
    private fun fetchWeatherByCoordinates(lat: Double, lon: Double, geocoderCityName: String) {
        val apiKey = BuildConfig.WEATHER_API_KEY
        val url = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
        executeWeatherApi(url, geocoderCityName)
    }

    // --- הבאת מזג אוויר לפי שם עיר (ברירת מחדל אם סירבו הרשאה) ---
    private fun fetchWeatherAndData(cityName: String) {
        val apiKey = "f233afe9d2abac01f01041b7a0c8dd2b" // שימי פה את המפתח שלך!
        val url = "https://api.openweathermap.org/data/2.5/weather?q=$cityName&appid=$apiKey&units=metric"
        executeWeatherApi(url,cityName)
    }

    // --- הפעלת הקריאה לשרת מזג האוויר ---
    private fun executeWeatherApi(url: String, cityOverride: String) {
        lifecycleScope.launch(Dispatchers.IO) {
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
                    binding.tvCityName.text = finalCityName.uppercase() // עכשיו זה יציג HAIFA!
                    binding.tvTemperature.text = "$temp°"
                    loadPostsFromFirestore(category)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Weather Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPostsFromFirestore(category: String) {
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("weatherCategory", category)
            .get()
            .addOnSuccessListener { result ->
                postsList.clear()
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    postsList.add(post)
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
