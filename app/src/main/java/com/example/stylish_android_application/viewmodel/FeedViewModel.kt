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

    init {
        loadInitialPosts()
    }

    fun loadInitialPosts() {
        if (isFetching) return
        isFetching = true

        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    currentPosts.clear()

                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            post.id = document.id
                            currentPosts.add(post)
                        }
                    }

                    // שומרים את המסמך האחרון כדי שנדע מאיפה להמשיך בפעם הבאה
                    lastVisibleDocument = snapshot.documents[snapshot.size() - 1]

                    // אם קיבלנו פחות פוסטים ממה שביקשנו, סימן שזה הסוף!
                    if (snapshot.size() < PAGE_SIZE) isLastPage = true

                    _postsList.value = ArrayList(currentPosts)
                }
                isFetching = false
            }
            .addOnFailureListener {
                isFetching = false
            }
    }

    fun loadMorePosts() {
        // אם אנחנו כבר טוענים, או שהגענו לסוף, או שאין נקודת התחלה - אל תעשה כלום
        if (isFetching || isLastPage || lastVisibleDocument == null) return

        isFetching = true

        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(lastVisibleDocument!!) // מתחילים בדיוק איפה שעצרנו!
            .limit(PAGE_SIZE)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            post.id = document.id
                            currentPosts.add(post)
                        }
                    }

                    lastVisibleDocument = snapshot.documents[snapshot.size() - 1]
                    if (snapshot.size() < PAGE_SIZE) isLastPage = true

                    _postsList.value = ArrayList(currentPosts) // מעדכנים את ה-UI
                } else {
                    isLastPage = true // אם הרשימה ריקה, הגענו לסוף
                }
                isFetching = false
            }
            .addOnFailureListener {
                isFetching = false
            }
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