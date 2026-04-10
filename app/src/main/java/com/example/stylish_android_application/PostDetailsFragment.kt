package com.example.stylish_android_application

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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private var currentPost: Post? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentPost = arguments?.getSerializable("post") as? Post
        currentPost?.let { render(it) }

        parentFragmentManager.setFragmentResultListener("post_edited", viewLifecycleOwner) { _, bundle ->
            val updatedPost = bundle.getSerializable("post") as? Post
            updatedPost?.let {
                currentPost = it
                render(it)
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val onUserClicked = View.OnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("USER_ID", currentPost?.userId)
            profileFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.fullPostCard.imgProfile.setOnClickListener(onUserClicked)
        binding.fullPostCard.lblUser.setOnClickListener(onUserClicked)

        binding.fullPostCard.btnComment.setOnClickListener {
            val post = currentPost ?: return@setOnClickListener
            CommentsBottomSheet.newInstance(post.id)
                .show(childFragmentManager, "comments")
        }

        childFragmentManager.setFragmentResultListener("comment_count_changed", viewLifecycleOwner) { _, bundle ->
            val newCount = bundle.getLong("commentCount")
            currentPost?.commentCount = newCount
            binding.fullPostCard.lblCommentCount.text = newCount.toString()
        }
    }

    private fun render(post: Post) {
        setupUI(post)
        setupDeleteButton(post)
        setupEditButton(post)
        setupLikeButton(post)
    }

    private fun setupUI(post: Post) {

        val card = binding.fullPostCard
        card.lblUser.text = post.userName
        card.lblLikeCount.text = post.likedBy.size.toString()
        card.lblCommentCount.text = post.commentCount.toString()

        // Load poster's profile image and latest username from Firestore
        card.imgProfile.setImageResource(R.drawable.img_profile)
        FirebaseFirestore.getInstance().collection("users").document(post.userId)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener
                val username = doc.getString("username")
                if (!username.isNullOrEmpty()) card.lblUser.text = username
                val url = doc.getString("profileImageUrl")
                if (!url.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(url)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.img_profile)
                        .into(card.imgProfile)
                }
            }

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
        setupBrandDetailView(post.brandGlasses, card.layoutGlasses, card.lblGlasses)
        setupBrandDetailView(post.brandAccessories, card.layoutAccessories, card.lblAccessories)
        setupBrandDetailView(post.occasion, card.layoutTarget, card.lbTarget)

        // --- טעינת התמונה המהירה דרך Glide ---
        if (post.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(post.imageUrl)
                // השורה החדשה: אומרת ל-Glide להשתמש בתמונה שכבר קיימת בזיכרון!
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_outfit)
                .into(card.imgMain)
        } else {
            card.imgMain.setImageResource(R.drawable.img_outfit)
        }
    }

    // --- לוגיקה לעריכת פוסט ---
    private fun setupEditButton(post: Post) {
        if (currentUserId == post.userId) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener {
                val editFragment = EditPostFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                editFragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            binding.btnEdit.visibility = View.GONE
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
        showConfirmDialog(
            context = requireContext(),
            title = "Delete Post",
            message = "Are you sure you want to delete this post?",
            positiveLabel = "Delete",
            onConfirm = { deletePostFromFirestore(post) }
        )
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

    private fun setupBrandDetailView(brandInput: String, container: View, textView: android.widget.TextView) {
        if (brandInput.isEmpty()) {
            container.visibility = View.GONE
            container.setOnClickListener(null)
        } else {
            container.visibility = View.VISIBLE
            if (BrandHelper.isUrl(brandInput)) {
                textView.text = BrandHelper.extractBrandName(brandInput)
                textView.paintFlags = textView.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                container.setOnClickListener { BrandHelper.openUrl(requireContext(), brandInput) }
            } else {
                textView.text = brandInput
                textView.paintFlags = textView.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                container.setOnClickListener(null)
            }
        }
    }
}