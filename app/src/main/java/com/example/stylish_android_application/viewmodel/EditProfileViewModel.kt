package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class UsernameUpdateState {
    object Idle : UsernameUpdateState()
    object Loading : UsernameUpdateState()
    object Success : UsernameUpdateState()
    data class Error(val message: String) : UsernameUpdateState()
}

class EditProfileViewModel : ViewModel() {

    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    private val _usernameUpdateState = MutableLiveData<UsernameUpdateState>(UsernameUpdateState.Idle)
    val usernameUpdateState: LiveData<UsernameUpdateState> = _usernameUpdateState

    fun uploadProfileImage(uid: String, imageBytes: ByteArray) {
        _uploadState.value = UploadState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
                storageRef.putBytes(imageBytes).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .set(hashMapOf("profileImageUrl" to downloadUrl), SetOptions.merge())
                    .await()
                UserRepository.updateCache(uid, imageUrl = downloadUrl)
                _uploadState.postValue(UploadState.Success)
            } catch (e: Exception) {
                _uploadState.postValue(UploadState.Error(e.message ?: "Failed to upload image"))
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }

    fun updateUsername(uid: String, newUsername: String) {
        val trimmed = newUsername.trim()
        if (trimmed.length < 2) {
            _usernameUpdateState.value = UsernameUpdateState.Error("Username must be at least 2 characters")
            return
        }
        if (trimmed.contains(" ")) {
            _usernameUpdateState.value = UsernameUpdateState.Error("Username cannot contain spaces")
            return
        }

        _usernameUpdateState.value = UsernameUpdateState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = FirebaseFirestore.getInstance()

                val existing = db.collection("users")
                    .whereEqualTo("username", trimmed).get().await()
                if (!existing.isEmpty && existing.documents[0].id != uid) {
                    _usernameUpdateState.postValue(UsernameUpdateState.Error("Username is already taken"))
                    return@launch
                }

                db.collection("users").document(uid)
                    .set(hashMapOf("username" to trimmed), SetOptions.merge()).await()

                val posts = db.collection("posts").whereEqualTo("userId", uid).get().await()
                if (!posts.isEmpty) {
                    val batch = db.batch()
                    posts.documents.forEach { doc -> batch.update(doc.reference, "userName", trimmed) }
                    batch.commit().await()
                }

                FirebaseAuth.getInstance().currentUser
                    ?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(trimmed).build())
                    ?.await()

                UserRepository.updateCache(uid, username = trimmed)
                _usernameUpdateState.postValue(UsernameUpdateState.Success)
            } catch (e: Exception) {
                _usernameUpdateState.postValue(UsernameUpdateState.Error(e.message ?: "Failed to update username"))
            }
        }
    }

    fun resetUsernameUpdateState() {
        _usernameUpdateState.value = UsernameUpdateState.Idle
    }
}
