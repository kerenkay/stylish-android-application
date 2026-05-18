package com.example.stylish_android_application.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stylish_android_application.BuildConfig
import com.example.stylish_android_application.model.Post
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class UploadState {
    object Idle : UploadState()
    object Loading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

class AddPostViewModel : ViewModel() {

    private val _uploadState = MutableLiveData<UploadState>(UploadState.Idle)
    val uploadState: LiveData<UploadState> = _uploadState

    fun uploadPost(
        bitmap: Bitmap,
        imageBytes: ByteArray,
        description: String,
        brandTop: String,
        brandBottom: String,
        occasion: String,
        brandJacket: String,
        brandShoes: String,
        brandBag: String,
        brandDress: String,
        brandGlasses: String,
        brandAccessories: String
    ) {
        // Notify the Fragment to show a loading indicator
        _uploadState.value = UploadState.Loading

        // viewModelScope ensures the coroutine survives configuration changes
        // and is automatically canceled if the ViewModel is cleared.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    _uploadState.postValue(UploadState.Error("User not logged in"))
                    return@launch
                }

                // 1. Upload compressed image to Firebase Storage
                val fileName = UUID.randomUUID().toString() + ".jpg"
                val storageRef = FirebaseStorage.getInstance().reference.child("post_images/$fileName")

                storageRef.putBytes(imageBytes).await()
                val imageUrl = storageRef.downloadUrl.await().toString()

                // 2. Analyze the outfit category with Gemini AI
                val aiCategory = analyzeOutfitWithAI(bitmap)

                // 3. Construct the Post object
                val userName = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Guest"
                val post = Post(
                    userId = currentUser.uid,
                    userName = userName,
                    imageUrl = imageUrl,
                    description = description,
                    brandTop = brandTop,
                    brandBottom = brandBottom,
                    occasion = occasion,
                    brandJacket = brandJacket,
                    brandShoes = brandShoes,
                    brandBag = brandBag,
                    brandDress = brandDress,
                    brandGlasses = brandGlasses,
                    brandAccessories = brandAccessories,
                    weatherCategory = aiCategory,
                    timestamp = System.currentTimeMillis()
                )

                // 4. Save the Post document to Firestore
                FirebaseFirestore.getInstance().collection("posts").add(post).await()

                FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                    .set(hashMapOf("username" to userName), SetOptions.merge())
                    .await()

                _uploadState.postValue(UploadState.Success)

            } catch (e: Exception) {
                _uploadState.postValue(UploadState.Error(e.message ?: "An error occurred during upload"))
            }
        }
    }

    private suspend fun analyzeOutfitWithAI(bitmap: Bitmap): String {
        return try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )

            val prompt = """
                Look at the clothing in this image. Classify the outfit into exactly one of these three weather categories: 
                - 'Hot' (for summer/hot weather, shorts, t-shirts, light dresses)
                - 'Warm' (for spring/autumn/mild weather, long sleeves, light jackets)
                - 'Cold' (for winter/freezing weather, heavy coats, sweaters, scarves)
                Reply with ONLY ONE WORD from the categories above (Hot, Warm, or Cold).
            """.trimIndent()

            val response = generativeModel.generateContent(content {
                image(bitmap)
                text(prompt)
            })

            val rawAnswer = response.text?.lowercase() ?: ""
            when {
                rawAnswer.contains("hot") -> "Hot"
                rawAnswer.contains("cold") -> "Cold"
                else -> "Warm" // Default fallback
            }
        } catch (e: Exception) {
            "Warm"
        }
    }

    // Call this after a successful upload to reset the state for the next post
    fun resetState() {
        _uploadState.value = UploadState.Idle
    }
}