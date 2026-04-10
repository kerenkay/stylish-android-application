package com.example.stylish_android_application

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

object UserRepository {

    data class UserProfile(val username: String = "", val imageUrl: String? = null)

    private val cache = ConcurrentHashMap<String, UserProfile>()

    suspend fun getUser(userId: String): UserProfile {
        cache[userId]?.let { return it }
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("users").document(userId).get().await()
            UserProfile(
                username = doc.getString("username") ?: "",
                imageUrl = doc.getString("profileImageUrl")
            ).also { cache[userId] = it }
        } catch (e: Exception) {
            UserProfile()
        }
    }

    /** Called by ProfileViewModel's snapshot listener to keep cache fresh. */
    fun updateCache(userId: String, username: String? = null, imageUrl: String? = null) {
        val existing = cache[userId] ?: UserProfile()
        cache[userId] = existing.copy(
            username = username ?: existing.username,
            imageUrl = imageUrl ?: existing.imageUrl
        )
    }

    fun invalidate(userId: String) = cache.remove(userId)
}
