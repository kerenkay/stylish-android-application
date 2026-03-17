package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {

    // --- LiveData for UI Observation ---

    // User Info
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    // Posts & Stats
    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _totalLikes = MutableLiveData<Int>()
    val totalLikes: LiveData<Int> = _totalLikes

    // Upload State (Reusing the state class we made earlier!)
    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    // Keep track of listeners to remove them when ViewModel dies
    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null

    fun loadUserProfile(targetUserId: String, isCurrentUser: Boolean) {
        // 1. Immediately set the name if it's the current user
        if (isCurrentUser) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val name = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
            _userName.value = name
        }

        // 2. Listen to the user document in Firestore
        userListener?.remove() // Remove old listener if exists
        userListener = FirebaseFirestore.getInstance().collection("users").document(targetUserId)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    if (!isCurrentUser) {
                        val name = document.getString("username")
                        if (!name.isNullOrEmpty()) {
                            _userName.value = name
                        }
                    }
                    _profileImageUrl.value = document.getString("profileImageUrl")
                } else {
                    _profileImageUrl.value = null
                }
            }
    }

    fun loadUserPosts(userId: String) {
        postsListener?.remove()
        postsListener = FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val postsList = mutableListOf<Post>()
                var likesCount = 0

                for (document in snapshot.documents) {
                    val post = document.toObject(Post::class.java)
                    if (post != null) {
                        post.id = document.id
                        postsList.add(post)
                        likesCount += post.likedBy.size
                    }
                }

                _userPosts.value = postsList
                _totalLikes.value = likesCount

                // Backup logic for foreign user name
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != currentUserId && postsList.isNotEmpty()) {
                    _userName.value = postsList[0].userName
                }
            }
    }

    fun uploadProfileImage(uid: String, imageBytes: ByteArray) {
        _uploadState.value = UploadState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")

                // Upload to Storage
                storageRef.putBytes(imageBytes).await()

                // Get URL
                val downloadUrl = storageRef.downloadUrl.await().toString()

                // Save to Firestore
                val userData = hashMapOf("profileImageUrl" to downloadUrl)
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(userData, SetOptions.merge())
                    .await()

                _uploadState.postValue(UploadState.Success)
            } catch (e: Exception) {
                _uploadState.postValue(UploadState.Error(e.message ?: "Failed to upload image"))
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    // Automatically called by Android when the Fragment is completely destroyed
    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        postsListener?.remove()
    }
}