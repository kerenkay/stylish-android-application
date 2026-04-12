package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.model.Post
import com.example.stylish_android_application.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    private val _postsList = MutableLiveData<List<Post>>()
    val postsList: LiveData<List<Post>> = _postsList

    private val _profileImages = MutableLiveData<Map<String, String?>>()
    val profileImages: LiveData<Map<String, String?>> = _profileImages

    private val currentPosts = mutableListOf<Post>()

    private val PAGE_SIZE = 10L
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isFetching = false // to prevent double calls
    var isLastPage = false
        private set
    private var followingList = listOf<String>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadFollowingListAndPosts()
    }

    private fun loadFollowingListAndPosts() {
        if (currentUserId == null) return

        FirebaseFirestore.getInstance().collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    followingList = document.get("following") as? List<String> ?: emptyList()
                }
                loadInitialPosts()
            }
            .addOnFailureListener {
                loadInitialPosts()
            }
    }

    fun loadInitialPosts() {
        if (isFetching || currentUserId == null) return
        isFetching = true

        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    currentPosts.clear()
                    val newPosts = mutableListOf<Post>()

                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            if (post.userId == currentUserId || followingList.contains(post.userId)) {
                                post.id = document.id
                                newPosts.add(post)
                            }
                        }
                    }

                    lastVisibleDocument = snapshot.documents[snapshot.size() - 1]
                    if (snapshot.size() < PAGE_SIZE) isLastPage = true
                    if (newPosts.isEmpty() && !isLastPage) {
                        isFetching = false
                        loadMorePosts()
                    } else {
                        currentPosts.addAll(newPosts)
                        _postsList.value = ArrayList(currentPosts)
                        loadProfileImages(newPosts)
                        isFetching = false
                    }
                } else {
                    isLastPage = true
                    isFetching = false
                }
            }
            .addOnFailureListener {
                isFetching = false
            }
    }

    fun loadMorePosts() {
        if (isFetching || isLastPage || lastVisibleDocument == null || currentUserId == null) return
        isFetching = true

        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleDocument!!)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val newPosts = mutableListOf<Post>()

                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            if (post.userId == currentUserId || followingList.contains(post.userId)) {
                                post.id = document.id
                                newPosts.add(post)
                            }
                        }
                    }

                    lastVisibleDocument = snapshot.documents[snapshot.size() - 1]
                    if (snapshot.size() < PAGE_SIZE) isLastPage = true

                    if (newPosts.isEmpty() && !isLastPage) {
                        isFetching = false
                        loadMorePosts()
                    } else {
                        currentPosts.addAll(newPosts)
                        _postsList.value = ArrayList(currentPosts)
                        loadProfileImages(newPosts)
                        isFetching = false
                    }
                } else {
                    isLastPage = true
                    isFetching = false
                }
            }
            .addOnFailureListener {
                isFetching = false
            }
    }

    fun refresh() {
        isFetching = false
        isLastPage = false
        lastVisibleDocument = null
        currentPosts.clear()
        _postsList.value = emptyList()
        loadFollowingListAndPosts()
    }

    private fun loadProfileImages(posts: List<Post>) {
        val knownIds = _profileImages.value?.keys ?: emptySet()
        val newIds = posts.map { it.userId }.distinct().filter { it !in knownIds }
        if (newIds.isEmpty()) return
        viewModelScope.launch {
            val current = _profileImages.value?.toMutableMap() ?: mutableMapOf()
            for (userId in newIds) {
                current[userId] = UserRepository.getUser(userId).imageUrl
            }
            _profileImages.postValue(current)
        }
    }

    fun updateCommentCount(postId: String, newCount: Long) {
        val updated = currentPosts.map { if (it.id == postId) it.copy(commentCount = newCount) else it }
        currentPosts.clear()
        currentPosts.addAll(updated)
        _postsList.value = ArrayList(currentPosts)
    }

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
}