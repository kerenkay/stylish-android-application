package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentSearchBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfileAdapter

    private val allPosts = mutableListOf<Post>()
    private val displayedPosts = mutableListOf<Post>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAllPosts()
        setupSearchListener()

        // עיצוב שורת החיפוש
        val searchEditText = binding.searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.textSize = 12f

        // --- הניווט החדש! מעבר למסך ההמלצות המעוצב בלחיצה ---
        binding.btnWeatherMatch.setOnClickListener {
            val weatherFragment = WeatherFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, weatherFragment)
                .addToBackStack(null) // מאפשר למשתמש ללחוץ "חזור" כדי לחזור לחיפוש
                .commit()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = GridLayoutManager(context, 3)

        adapter = ProfileAdapter(
            displayedPosts,
            onPostClick = { post ->
                val fragment = PostDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onPostLongClick = {}
        )
        binding.rvSearchResults.adapter = adapter
    }

    private fun loadAllPosts() {
        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allPosts.clear()
                displayedPosts.clear()

                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    allPosts.add(post)
                }

                displayedPosts.addAll(allPosts)
                adapter.notifyDataSetChanged()
            }
    }

    private fun setupSearchListener() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText)
                return true
            }
        })
    }

    // --- הפונקציה החכמה המסננת ששמרנו ---
    private fun filterPosts(query: String?) {
        displayedPosts.clear()

        if (query.isNullOrEmpty()) {
            displayedPosts.addAll(allPosts)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.ROOT).trim()

            when (lowerCaseQuery) {
                "winter" -> {
                    displayedPosts.addAll(allPosts.filter { it.weatherCategory.equals("Cold", ignoreCase = true) })
                }
                "spring", "autumn", "fall" -> {
                    displayedPosts.addAll(allPosts.filter { it.weatherCategory.equals("Warm", ignoreCase = true) })
                }
                "summer" -> {
                    displayedPosts.addAll(allPosts.filter { it.weatherCategory.equals("Hot", ignoreCase = true) })
                }
                else -> {
                    val filteredList = allPosts.filter { post ->
                        post.userName.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                                post.brandTop.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                                post.brandBottom.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                                post.occasion.lowercase(Locale.ROOT).contains(lowerCaseQuery)
                    }
                    displayedPosts.addAll(filteredList)
                }
            }
        }

        if (displayedPosts.isEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}