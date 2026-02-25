package com.example.stylish_android_application
import java.io.Serializable
data class Post(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "", // המחרוזת הארוכה של התמונה
    val description: String = "",
    val brandTop: String = "",
    val brandBottom: String = "",
    val occasion: String = "",
    val likedBy: ArrayList<String> = ArrayList(),
    val timestamp: Long = 0
): Serializable