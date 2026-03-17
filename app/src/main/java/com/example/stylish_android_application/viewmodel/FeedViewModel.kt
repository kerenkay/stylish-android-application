package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FeedViewModel : ViewModel() {

    // The single source of truth for our feed
    private val _postsList = MutableLiveData<List<Post>>()
    val postsList: LiveData<List<Post>> = _postsList

    // Store the listener so we can remove it when the ViewModel dies to prevent memory leaks
    private var postsListener: ListenerRegistration? = null

    init {
        // Start fetching posts as soon as the ViewModel is created
        loadPosts()
    }

    private fun loadPosts() {
        postsListener?.remove() // Remove any existing listener just in case

        postsListener = FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    return@addSnapshotListener
                }

                val posts = mutableListOf<Post>()
                for (document in snapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    if (post != null) {
                        post.id = document.id // Important: saving the document ID!
                        posts.add(post)
                    }
                }

                // Update the LiveData which will automatically update the UI
                _postsList.value = posts
            }
    }

    // Handles the like logic directly in the database
    fun toggleLike(post: Post) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val postRef = FirebaseFirestore.getInstance().collection("posts").document(post.id)

        val isLiked = post.likedBy.contains(currentUser.uid)

        if (isLiked) {
            postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid))
        } else {
            postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid))
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up the listener when the ViewModel is destroyed
        postsListener?.remove()
    }
}