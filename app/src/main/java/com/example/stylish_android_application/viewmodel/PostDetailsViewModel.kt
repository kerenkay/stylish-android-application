package com.example.stylish_android_application.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.Post
import com.example.stylish_android_application.PostRepository
import com.example.stylish_android_application.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

sealed class PostDetailsState {
    object Idle : PostDetailsState()
    object Deleted : PostDetailsState()
    data class Error(val message: String) : PostDetailsState()
}

class PostDetailsViewModel : ViewModel() {

    private val postRepo = PostRepository()

    private val _authorProfile = MutableLiveData<UserRepository.UserProfile>()
    val authorProfile: LiveData<UserRepository.UserProfile> = _authorProfile

    private val _state = MutableLiveData<PostDetailsState>(PostDetailsState.Idle)
    val state: LiveData<PostDetailsState> = _state

    fun loadAuthorProfile(userId: String) {
        viewModelScope.launch {
            _authorProfile.postValue(UserRepository.getUser(userId))
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                postRepo.deletePost(post.id)
                _state.postValue(PostDetailsState.Deleted)
            } catch (e: Exception) {
                _state.postValue(PostDetailsState.Error(e.message ?: "Failed to delete post"))
            }
        }
    }

    fun toggleLike(post: Post) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        postRepo.toggleLike(post.id, userId, post.likedBy.contains(userId))
    }

    fun resetState() {
        _state.value = PostDetailsState.Idle
    }
}
