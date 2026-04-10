package com.example.stylish_android_application

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PostRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
    }

    fun toggleLike(postId: String, userId: String, isCurrentlyLiked: Boolean) {
        val postRef = db.collection("posts").document(postId)
        if (isCurrentlyLiked) {
            postRef.update("likedBy", FieldValue.arrayRemove(userId))
        } else {
            postRef.update("likedBy", FieldValue.arrayUnion(userId))
        }
    }
}
