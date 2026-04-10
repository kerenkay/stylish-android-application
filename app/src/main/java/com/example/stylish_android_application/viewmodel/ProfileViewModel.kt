package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.Post
import com.example.stylish_android_application.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ProfileViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _profileImageUrl = MutableLiveData<String?>()
    val profileImageUrl: LiveData<String?> = _profileImageUrl

    private val _followersCount = MutableLiveData<Int>()
    val followersCount: LiveData<Int> = _followersCount

    private val _followingCount = MutableLiveData<Int>()
    val followingCount: LiveData<Int> = _followingCount

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _totalLikes = MutableLiveData<Int>()
    val totalLikes: LiveData<Int> = _totalLikes

    private var userListener: ListenerRegistration? = null
    private var postsListener: ListenerRegistration? = null

    fun loadUserProfile(targetUserId: String, isCurrentUser: Boolean) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (isCurrentUser) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val name = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
            _userName.value = name
        }

        userListener?.remove()
        userListener = FirebaseFirestore.getInstance().collection("users").document(targetUserId)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener
                if (document != null && document.exists()) {
                    val username = document.getString("username")
                    val imageUrl = document.getString("profileImageUrl")

                    // Keep UserRepository cache fresh for the rest of the app
                    UserRepository.updateCache(targetUserId, username = username, imageUrl = imageUrl)

                    if (!isCurrentUser && !username.isNullOrEmpty()) {
                        _userName.value = username
                    }
                    _profileImageUrl.value = imageUrl

                    val followers = document.get("followers") as? List<String> ?: emptyList()
                    val following = document.get("following") as? List<String> ?: emptyList()
                    _followersCount.value = followers.size
                    _followingCount.value = following.size
                    _isFollowing.value = followers.contains(currentUserId)
                } else {
                    _profileImageUrl.value = null
                    _followersCount.value = 0
                    _followingCount.value = 0
                    _isFollowing.value = false
                }
            }
    }

    fun toggleFollow(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val targetUserRef = db.collection("users").document(targetUserId)
        val currentUserRef = db.collection("users").document(currentUserId)
        val currentlyFollowing = _isFollowing.value ?: false

        db.runBatch { batch ->
            if (currentlyFollowing) {
                batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
            } else {
                batch.set(targetUserRef, hashMapOf("followers" to FieldValue.arrayUnion(currentUserId)), SetOptions.merge())
                batch.set(currentUserRef, hashMapOf("following" to FieldValue.arrayUnion(targetUserId)), SetOptions.merge())
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

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != currentUserId && postsList.isNotEmpty()) {
                    _userName.value = postsList[0].userName
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
        postsListener?.remove()
    }
}
