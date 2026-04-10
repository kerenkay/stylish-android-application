package com.example.stylish_android_application

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CommentAdapter(
    private var comments: List<Comment>,
    private val onDeleteClicked: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val profileImageCache = HashMap<String, String?>()

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.binding.lblCommentUser.text = comment.userName
        holder.binding.lblCommentText.text = comment.text

        if (comment.userId == currentUserId) {
            holder.itemView.setOnLongClickListener {
                showConfirmDialog(
                    context = holder.itemView.context,
                    title = "Delete Comment",
                    message = "Are you sure you want to delete this comment?",
                    positiveLabel = "Delete",
                    onConfirm = { onDeleteClicked(comment) }
                )
                true
            }
        } else {
            holder.itemView.setOnLongClickListener(null)
        }

        holder.binding.imgCommentProfile.setImageResource(R.drawable.img_profile)
        holder.binding.imgCommentProfile.tag = comment.userId

        val cached = profileImageCache[comment.userId]
        if (profileImageCache.containsKey(comment.userId)) {
            if (!cached.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(cached)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.img_profile)
                    .into(holder.binding.imgCommentProfile)
            }
        } else {
            FirebaseFirestore.getInstance().collection("users").document(comment.userId)
                .get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("profileImageUrl")
                    profileImageCache[comment.userId] = url
                    if (!url.isNullOrEmpty() && holder.binding.imgCommentProfile.tag == comment.userId) {
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.img_profile)
                            .into(holder.binding.imgCommentProfile)
                    }
                }
        }
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }
}
