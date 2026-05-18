package com.example.stylish_android_application.model

data class Comment(
    val commentId: String = "",
    val userId: String = "",
    val userName: String = "",
    val text: String = "",
    val timestamp: Long = 0
)