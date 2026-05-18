package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class SearchViewModel : ViewModel() {

    private val _displayedPosts = MutableLiveData<List<Post>>()
    val displayedPosts: LiveData<List<Post>> = _displayedPosts
    private val allPosts = mutableListOf<Post>()

    init {
        loadAllPosts()
    }

    private fun loadAllPosts() {
        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allPosts.clear()
                val postsList = mutableListOf<Post>()
                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    postsList.add(post)
                    allPosts.add(post)
                }
                _displayedPosts.value = postsList
            }
    }

    fun reload() {
        allPosts.clear()
        _displayedPosts.value = emptyList()
        loadAllPosts()
    }

    fun filterPosts(query: String?) {
        if (query.isNullOrEmpty()) {
            _displayedPosts.value = allPosts
            return
        }

        val lowerCaseQuery = query.lowercase(Locale.ROOT).trim()
        val filteredList = mutableListOf<Post>()

        when (lowerCaseQuery) {
            "winter" -> filteredList.addAll(allPosts.filter { it.weatherCategory.equals("Cold", ignoreCase = true) })
            "spring", "autumn", "fall" -> filteredList.addAll(allPosts.filter { it.weatherCategory.equals("Warm", ignoreCase = true) })
            "summer" -> filteredList.addAll(allPosts.filter { it.weatherCategory.equals("Hot", ignoreCase = true) })
            else -> {
                filteredList.addAll(allPosts.filter { post ->
                    post.userName.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                            post.brandTop.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                            post.brandBottom.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                            post.occasion.lowercase(Locale.ROOT).contains(lowerCaseQuery)
                })
            }
        }
        _displayedPosts.value = filteredList
    }
}