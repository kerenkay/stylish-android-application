package com.example.stylish_android_application

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.stylish_android_application.databinding.FragmentPostDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val post = arguments?.getSerializable("post") as? Post

        post?.let { selectedPost ->
            setupUI(selectedPost)
            setupDeleteButton(selectedPost)
            setupLikeButton(selectedPost)
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupUI(post: Post) {
        // גישה לשדות בתוך ה-Include דרך fullPostCard
        binding.fullPostCard.lblUser.text = post.userName
        binding.fullPostCard.lblDescription.text = post.description
        binding.fullPostCard.lblTop.text = post.brandTop
        binding.fullPostCard.lblBottom.text = post.brandBottom
        binding.fullPostCard.lbTarget.text = post.occasion
        binding.fullPostCard.lblLikeCount.text = "${post.likedBy.size} likes"

        if (post.imageUrl.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)
                // שימוש בפונקציית עזר להקטנת תמונה אם יש צורך, או טעינה ישירה
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                binding.fullPostCard.imgMain.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                binding.fullPostCard.imgMain.setImageResource(R.drawable.img_outfit)
            }
        }
    }

    // --- לוגיקה למחיקת פוסט ---
    private fun setupDeleteButton(post: Post) {
        // 1. בדיקה: האם הפוסט שייך למשתמש המחובר?
        if (currentUserId == post.userId) {
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                showDeleteConfirmationDialog(post)
            }
        } else {
            // אם זה לא הפוסט שלי - להסתיר את הפח
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun showDeleteConfirmationDialog(post: Post) {
        AlertDialog.Builder(context)
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post?")
            .setPositiveButton("Delete") { _, _ ->
                deletePostFromFirestore(post)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePostFromFirestore(post: Post) {
        FirebaseFirestore.getInstance().collection("posts")
            .document(post.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack() // חזרה למסך הקודם
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete post", Toast.LENGTH_SHORT).show()
            }
    }

    // --- לוגיקה ללייקים ---
    private fun setupLikeButton(post: Post) {
        // עדכון ראשוני של מצב הלב
        updateLikeIcon(post)

        // טיפול בלחיצה על הלב
        binding.fullPostCard.btnLike.setOnClickListener {
            if (currentUserId == null) return@setOnClickListener

            val db = FirebaseFirestore.getInstance()
            val postRef = db.collection("posts").document(post.id)
            val isLiked = post.likedBy.contains(currentUserId)

            if (isLiked) {
                // ביטול לייק
                post.likedBy.remove(currentUserId) // עדכון מקומי מהיר
                postRef.update("likedBy", FieldValue.arrayRemove(currentUserId))
            } else {
                // הוספת לייק
                post.likedBy.add(currentUserId!!) // עדכון מקומי מהיר
                postRef.update("likedBy", FieldValue.arrayUnion(currentUserId))
            }

            // עדכון התצוגה (אייקון ומספר)
            updateLikeIcon(post)
            binding.fullPostCard.lblLikeCount.text = post.likedBy.size.toString()
        }
    }

    private fun updateLikeIcon(post: Post) {
        val isLiked = post.likedBy.contains(currentUserId)
        if (isLiked) {
            binding.fullPostCard.btnLike.setIconResource(R.drawable.ic_heart_full) // לב מלא
            binding.fullPostCard.btnLike.setIconTintResource(R.color.red) // אדום
        } else {
            binding.fullPostCard.btnLike.setIconResource(R.drawable.ic_heart) // לב ריק
            binding.fullPostCard.btnLike.setIconTintResource(android.R.color.black) // שחור
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}