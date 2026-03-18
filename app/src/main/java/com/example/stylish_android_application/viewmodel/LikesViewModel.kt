package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.FolderItem
import com.example.stylish_android_application.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class LikesViewModel : ViewModel() {

    // The single source of truth for the UI - a ready-to-use list of folders!
    private val _folders = MutableLiveData<List<FolderItem>>()
    val folders: LiveData<List<FolderItem>> = _folders
    var openedFolderName: String? = null

    private var snapshotListener: ListenerRegistration? = null

    init {
        // Start loading the saved posts as soon as the ViewModel is created
        loadLikedPosts()
    }

    private fun loadLikedPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        snapshotListener?.remove()
        snapshotListener = FirebaseFirestore.getInstance().collection("posts")
            .whereArrayContains("likedBy", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val allLikedPosts = mutableListOf<Post>()
                for (document in snapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    if (post != null) {
                        post.id = document.id
                        allLikedPosts.add(post)
                    }
                }

                // Once we have the posts, group them into folders
                groupPostsIntoFolders(allLikedPosts)
            }
    }

    private fun groupPostsIntoFolders(allLikedPosts: List<Post>) {
        val newFolders = mutableListOf<FolderItem>()

        if (allLikedPosts.isNotEmpty()) {
            // 1. Create the master "All Saved" folder
            newFolders.add(FolderItem("All Saved", allLikedPosts))

            // 2. Group the rest by occasion
            val groupedByOccasion = allLikedPosts.groupBy { it.occasion }

            for ((occasion, posts) in groupedByOccasion) {
                if (occasion.isNotEmpty()) {
                    // Capitalize the first letter for aesthetics
                    val formattedName = occasion.replaceFirstChar { it.uppercase() }
                    newFolders.add(FolderItem(formattedName, posts))
                }
            }
        }

        // Update the LiveData!
        _folders.value = newFolders
    }

    override fun onCleared() {
        super.onCleared()
        snapshotListener?.remove() // Prevent memory leaks
    }
}