package com.example.stylish_android_application.model

import java.io.Serializable

data class Post(
    var id: String = "",
    val userId: String = "",
    val userName: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val brandTop: String = "",
    val brandBottom: String = "",
    val occasion: String = "",
    val brandJacket: String = "",
    val brandShoes: String = "",
    val brandBag: String = "",
    val brandDress: String = "",
    val brandGlasses: String = "",
    val brandAccessories: String = "",
    val weatherCategory: String = "",
    var likedBy: ArrayList<String> = ArrayList(),
    val timestamp: Long = 0,
    var commentCount: Long = 0
): Serializable