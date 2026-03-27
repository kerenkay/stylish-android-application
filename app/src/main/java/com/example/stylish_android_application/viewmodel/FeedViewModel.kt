package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedViewModel : ViewModel() {

    private val _postsList = MutableLiveData<List<Post>>()
    val postsList: LiveData<List<Post>> = _postsList

    // הרשימה המצטברת של הפוסטים בזיכרון
    private val currentPosts = mutableListOf<Post>()

    // משתני ניהול Pagination
    private val PAGE_SIZE = 10L
    private var lastVisibleDocument: DocumentSnapshot? = null
    private var isFetching = false // מונע קריאות כפולות אם המשתמש גולל מהר
    var isLastPage = false // מסמן אם הגענו לסוף מסד הנתונים
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
                            // הסינון החכם חל גם על טעינת ההמשך בגלילה למטה
                            if (post.userId == currentUserId || followingList.contains(post.userId)) {
                                post.id = document.id
                                newPosts.add(post)
                            }
                        }
                    }

                    lastVisibleDocument = snapshot.documents[snapshot.size() - 1]
                    if (snapshot.size() < PAGE_SIZE) isLastPage = true

                    // דילוג אוטומטי במקרה של מנה ריקה
                    if (newPosts.isEmpty() && !isLastPage) {
                        isFetching = false
                        loadMorePosts()
                    } else {
                        currentPosts.addAll(newPosts)
                        _postsList.value = ArrayList(currentPosts)
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