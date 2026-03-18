package com.example.stylish_android_application

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentWeatherBinding
import com.example.stylish_android_application.viewmodel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class WeatherFragment : Fragment() {

    private var _binding: FragmentWeatherBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfileAdapter
    private lateinit var viewModel: WeatherViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission denied. Using default city.", Toast.LENGTH_SHORT).show()
            viewModel.fetchWeatherAndData("Haifa")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWeatherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupObservers()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        checkLocationPermission()
    }

    private fun setupRecyclerView() {
        binding.rvWeatherPosts.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfileAdapter(emptyList()) { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvWeatherPosts.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.weatherPosts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
        }

        viewModel.cityName.observe(viewLifecycleOwner) { name ->
            binding.tvCityName.text = name
        }

        viewModel.temperature.observe(viewLifecycleOwner) { temp ->
            binding.tvTemperature.text = temp
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (msg != null) {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                var realCityName = ""
                try {
                    val geocoder = Geocoder(requireContext(), Locale.ENGLISH)
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        realCityName = addresses[0].locality ?: addresses[0].subAdminArea ?: ""
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                viewModel.fetchWeatherByCoordinates(location.latitude, location.longitude, realCityName)
            } else {
                viewModel.fetchWeatherAndData("Haifa")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}