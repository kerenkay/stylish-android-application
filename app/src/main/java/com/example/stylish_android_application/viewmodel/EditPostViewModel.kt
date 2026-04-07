package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

sealed class EditState {
    object Idle : EditState()
    object Loading : EditState()
    object Success : EditState()
    data class Error(val message: String) : EditState()
}

class EditPostViewModel : ViewModel() {

    private val _editState = MutableLiveData<EditState>(EditState.Idle)
    val editState: LiveData<EditState> = _editState

    fun updatePost(
        postId: String,
        description: String,
        brandTop: String,
        brandBottom: String,
        brandJacket: String,
        brandShoes: String,
        brandBag: String,
        brandDress: String,
        brandGlasses: String,
        brandAccessories: String,
        occasion: String
    ) {
        _editState.value = EditState.Loading
        val updates = mapOf(
            "description" to description,
            "brandTop" to brandTop,
            "brandBottom" to brandBottom,
            "brandJacket" to brandJacket,
            "brandShoes" to brandShoes,
            "brandBag" to brandBag,
            "brandDress" to brandDress,
            "brandGlasses" to brandGlasses,
            "brandAccessories" to brandAccessories,
            "occasion" to occasion
        )
        FirebaseFirestore.getInstance().collection("posts").document(postId)
            .update(updates)
            .addOnSuccessListener { _editState.value = EditState.Success }
            .addOnFailureListener { _editState.value = EditState.Error(it.message ?: "Update failed") }
    }
}
