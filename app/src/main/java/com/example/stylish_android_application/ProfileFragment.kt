package com.example.stylish_android_application

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userPosts = mutableListOf<Post>()
    private lateinit var adapter: ProfileAdapter

    // משתנה לבחירת תמונה מהגלריה
    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadProfileImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUserProfile()
        setupRecyclerView()
        loadMyPosts()

        // לחיצה על תמונת הפרופיל
        binding.imgProfile.setOnClickListener {
            pickProfileImage.launch("image/*")
        }
    }

    private fun setupUserProfile() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "User"
            binding.tvUserName.text = name

            // טעינת תמונת הפרופיל השמורה
            loadProfileImage(currentUser.uid)
        }
    }

    // טעינת התמונה מ-Firestore
    private fun loadProfileImage(userId: String) {
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val base64Image = document.getString("profileImageUrl")
                    if (!base64Image.isNullOrEmpty()) {
                        val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        binding.imgProfile.setImageBitmap(decodedBitmap)
                    }
                }
            }
    }

    // העלאת תמונת פרופיל ושמירה
    private fun uploadProfileImage(uri: android.net.Uri) {
        try {
            // שימוש בפונקציה החדשה שמסובבת ומקטינה את התמונה!
            val finalBitmap = getRotatedProfileBitmap(uri) ?: return

            val baos = java.io.ByteArrayOutputStream()
            finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
            val base64Image = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)

            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

            // שמירה ב-Firestore
            val userData = hashMapOf("profileImageUrl" to base64Image)
            FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener {
                    // עדכון העיגול בתצוגה לתמונה הישרה
                    binding.imgProfile.setImageBitmap(finalBitmap)
                    Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        binding.rvProfilePosts.layoutManager = GridLayoutManager(context, 3)

        adapter = ProfileAdapter(
            userPosts,
            onPostClick = { post ->
                val fragment = PostDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // ודאי שה-ID תואם ל-activity_main
                    .addToBackStack(null)
                    .commit()
            },
            onPostLongClick = { post -> showDeleteDialog(post) }
        )
        binding.rvProfilePosts.adapter = adapter
    }

    private fun loadMyPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    userPosts.clear()
                    var totalLikes = 0 // משתנה לספירה

                    for (document in value) {
                        val post = document.toObject(Post::class.java)
                        post.id = document.id
                        userPosts.add(post)

                        // חישוב הלייקים
                        totalLikes += post.likedBy.size
                    }
                    adapter.notifyDataSetChanged()

                    // עדכון התצוגה
                    binding.tvPostsCount.text = "${userPosts.size} Posts"
                    binding.tvTotalLikes.text = "$totalLikes Likes"
                }
            }
    }

    private fun showDeleteDialog(post: Post) {
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this outfit?")
            .setPositiveButton("Delete") { _, _ -> deletePost(post) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        FirebaseFirestore.getInstance().collection("posts")
            .document(post.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                // אין צורך למחוק ידנית, ה-SnapshotListener יעשה את זה לבד
            }
    }

    private fun getRotatedProfileBitmap(uri: android.net.Uri): android.graphics.Bitmap? {
        // קריאת התמונה מהזיכרון
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        var bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap == null) return null

        // הקטנה משמעותית לפרופיל (300x300)
        val maxDimension = 300
        if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // סיבוב התמונה לפי המידע הנסתר (Exif)
        var rotatedBitmap = bitmap
        try {
            val exifInputStream = requireContext().contentResolver.openInputStream(uri)
            if (exifInputStream != null) {
                val exif = android.media.ExifInterface(exifInputStream)
                val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)

                val matrix = android.graphics.Matrix()
                when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL && orientation != android.media.ExifInterface.ORIENTATION_UNDEFINED) {
                    rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                exifInputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return rotatedBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}