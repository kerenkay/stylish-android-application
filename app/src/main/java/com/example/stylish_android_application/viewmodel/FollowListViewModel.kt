package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FollowListViewModel : ViewModel() {

    data class UserItem(
        val id: String,
        var name: String,
        val profileImageUrl: String,
        var isFollowing: Boolean
    )

    private val allUsers = mutableListOf<UserItem>()
    private var currentQuery: String = ""

    private val _filteredUsers = MutableLiveData<List<UserItem>>()
    val filteredUsers: LiveData<List<UserItem>> = _filteredUsers

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUsers(targetUserId: String, mode: String) {
        _isLoading.value = true
        val db = FirebaseFirestore.getInstance()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { currentUserDoc ->
                val myFollowing = currentUserDoc.get("following") as? List<String> ?: emptyList()

                db.collection("users").document(targetUserId).get()
                    .addOnSuccessListener { targetDoc ->
                        val userIds = if (mode == "FOLLOWERS") {
                            targetDoc.get("followers") as? List<String> ?: emptyList()
                        } else {
                            targetDoc.get("following") as? List<String> ?: emptyList()
                        }

                        if (userIds.isEmpty()) {
                            allUsers.clear()
                            publishFiltered()
                            _isLoading.value = false
                            return@addOnSuccessListener
                        }

                        val results = mutableListOf<UserItem>()
                        var remaining = userIds.size

                        for (uid in userIds) {
                            val isFollowing = myFollowing.contains(uid)

                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { userDoc ->
                                    val imageUrl = userDoc.getString("profileImageUrl") ?: ""
                                    val nameFromDoc = userDoc.getString("username") ?: ""

                                    if (nameFromDoc.isNotEmpty()) {
                                        // User document has a username — use it directly
                                        results.add(UserItem(uid, nameFromDoc, imageUrl, isFollowing))
                                        remaining--
                                        if (remaining == 0) finalizeResults(results)
                                    } else {
                                        // Fallback: get username from their latest post
                                        db.collection("posts")
                                            .whereEqualTo("userId", uid)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener { postsSnap ->
                                                val nameFromPost = if (!postsSnap.isEmpty)
                                                    postsSnap.documents[0].getString("userName") ?: uid
                                                else uid
                                                results.add(UserItem(uid, nameFromPost, imageUrl, isFollowing))
                                                remaining--
                                                if (remaining == 0) finalizeResults(results)
                                            }
                                            .addOnFailureListener {
                                                results.add(UserItem(uid, uid, imageUrl, isFollowing))
                                                remaining--
                                                if (remaining == 0) finalizeResults(results)
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    results.add(UserItem(uid, uid, "", isFollowing))
                                    remaining--
                                    if (remaining == 0) finalizeResults(results)
                                }
                        }
                    }
                    .addOnFailureListener { _isLoading.value = false }
            }
            .addOnFailureListener { _isLoading.value = false }
    }

    private fun finalizeResults(results: MutableList<UserItem>) {
        allUsers.clear()
        allUsers.addAll(results.sortedBy { it.name })
        publishFiltered()
        _isLoading.value = false
    }

    fun filterUsers(query: String) {
        currentQuery = query
        publishFiltered()
    }

    fun toggleFollow(targetUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val item = allUsers.find { it.id == targetUserId } ?: return
        val currentlyFollowing = item.isFollowing

        val targetUserRef = db.collection("users").document(targetUserId)
        val currentUserRef = db.collection("users").document(currentUserId)

        db.runBatch { batch ->
            if (currentlyFollowing) {
                batch.update(targetUserRef, "followers", FieldValue.arrayRemove(currentUserId))
                batch.update(currentUserRef, "following", FieldValue.arrayRemove(targetUserId))
            } else {
                batch.set(targetUserRef, hashMapOf("followers" to FieldValue.arrayUnion(currentUserId)), SetOptions.merge())
                batch.set(currentUserRef, hashMapOf("following" to FieldValue.arrayUnion(targetUserId)), SetOptions.merge())
            }
        }.addOnSuccessListener {
            item.isFollowing = !currentlyFollowing
            publishFiltered()
        }
    }

    private fun publishFiltered() {
        val query = currentQuery.trim().lowercase()
        _filteredUsers.value = if (query.isEmpty()) {
            allUsers.toList()
        } else {
            allUsers.filter { it.name.lowercase().contains(query) }
        }
    }
}
