package com.example.stylish_android_application.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.model.Comment
import com.example.stylish_android_application.R
import com.example.stylish_android_application.databinding.ItemCommentBinding
import com.example.stylish_android_application.utils.showConfirmDialog
import com.google.firebase.auth.FirebaseAuth

class CommentAdapter(
    private var comments: List<Comment>,
    private val onDeleteClicked: (Comment) -> Unit
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid
    private var profileImages: Map<String, String?> = emptyMap()

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
        val imageUrl = profileImages[comment.userId]
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_profile)
                .into(holder.binding.imgCommentProfile)
        }
    }

    override fun getItemCount() = comments.size

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    fun updateProfileImages(images: Map<String, String?>) {
        profileImages = images
        notifyDataSetChanged()
    }
}
