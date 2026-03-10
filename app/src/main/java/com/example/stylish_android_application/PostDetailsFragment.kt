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

        val onUserClicked = View.OnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("USER_ID", post?.userId)
            profileFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        // 2. מחברים את הלחיצה לרכיבים!
        // שימי לב שאנחנו ניגשים אליהם דרך 'fullPostCard'
//        binding.fullPostCard.imgProfile.setOnClickListener(onUserClicked)
        binding.fullPostCard.lblUser.setOnClickListener(onUserClicked)
    }

    private fun setupUI(post: Post) {

        val card = binding.fullPostCard
        card.lblUser.text = post.userName
        card.lblLikeCount.text = post.likedBy.size.toString()

        if (post.description.isEmpty()) {
            card.lblDescription.visibility = View.GONE
        } else {
            card.lblDescription.visibility = View.VISIBLE
            card.lblDescription.text = post.description
        }

        setupBrandDetailView(post.brandTop, card.layoutTop, card.lblTop)
        setupBrandDetailView(post.brandBottom, card.layoutBottom, card.lblBottom)
        setupBrandDetailView(post.brandDress, card.layoutDress, card.lblDress)
        setupBrandDetailView(post.brandJacket, card.layoutJacket, card.lblJacket)
        setupBrandDetailView(post.brandShoes, card.layoutShoes, card.lblShoes)
        setupBrandDetailView(post.brandBag, card.layoutBag, card.lblBag)
        setupBrandDetailView(post.occasion, card.layoutTarget, card.lbTarget)

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

    private fun setupBrandDetailView(brandName: String, container: View, textView: android.widget.TextView) {
        if (brandName.isEmpty()) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            textView.text = brandName
        }
    }
}