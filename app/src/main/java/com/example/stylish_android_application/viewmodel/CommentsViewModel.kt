package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.stylish_android_application.Comment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class CommentsViewModel : ViewModel() {

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    private val _commentCount = MutableLiveData<Long>()
    val commentCount: LiveData<Long> = _commentCount

    private var commentsListener: ListenerRegistration? = null

    fun loadComments(postId: String) {
        commentsListener?.remove()
        commentsListener = FirebaseFirestore.getInstance()
            .collection("posts").document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Comment::class.java)?.copy(commentId = doc.id)
                }
                _comments.value = list
                _commentCount.value = list.size.toLong()
            }
    }

    fun addComment(postId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(postId)
        val commentsRef = postRef.collection("comments")

        val comment = hashMapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"),
            "text" to trimmed,
            "timestamp" to System.currentTimeMillis()
        )

        db.runBatch { batch ->
            val newCommentRef = commentsRef.document()
            batch.set(newCommentRef, comment)
            batch.update(postRef, "commentCount", FieldValue.increment(1))
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(postId)
        db.runBatch { batch ->
            batch.delete(postRef.collection("comments").document(commentId))
            batch.update(postRef, "commentCount", FieldValue.increment(-1))
        }
    }

    override fun onCleared() {
        super.onCleared()
        commentsListener?.remove()
    }
}
