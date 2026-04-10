package com.example.stylish_android_application

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth

class PostsAdapter(
    private var posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onUserClicked: (String) -> Unit,
    private val onCommentClicked: (Post) -> Unit = {}
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    // Always read fresh from FirebaseAuth so it's never stale after re-login
    private val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid

    private var profileImages: Map<String, String?> = emptyMap()

    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.binding.lblUser.text = post.userName
        holder.binding.lblLikeCount.text = post.likedBy.size.toString()
        holder.binding.lblCommentCount.text = post.commentCount.toString()
        holder.binding.btnComment.setOnClickListener { onCommentClicked(post) }

        // Profile image — no Firestore, use pre-loaded map
        holder.binding.imgProfile.setImageResource(R.drawable.img_profile)
        val imageUrl = profileImages[post.userId]
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_profile)
                .into(holder.binding.imgProfile)
        }

        holder.binding.imgProfile.setOnClickListener { onUserClicked(post.userId) }
        holder.binding.lblUser.setOnClickListener { onUserClicked(post.userId) }

        if (post.description.isEmpty()) {
            holder.binding.lblDescription.visibility = View.GONE
        } else {
            holder.binding.lblDescription.visibility = View.VISIBLE
            holder.binding.lblDescription.text = post.description
        }

        val ctx = holder.itemView.context
        setupBrandView(ctx, post.brandTop, holder.binding.layoutTop, holder.binding.lblTop)
        setupBrandView(ctx, post.brandBottom, holder.binding.layoutBottom, holder.binding.lblBottom)
        setupBrandView(ctx, post.brandDress, holder.binding.layoutDress, holder.binding.lblDress)
        setupBrandView(ctx, post.brandJacket, holder.binding.layoutJacket, holder.binding.lblJacket)
        setupBrandView(ctx, post.brandShoes, holder.binding.layoutShoes, holder.binding.lblShoes)
        setupBrandView(ctx, post.brandBag, holder.binding.layoutBag, holder.binding.lblBag)
        setupBrandView(ctx, post.brandGlasses, holder.binding.layoutGlasses, holder.binding.lblGlasses)
        setupBrandView(ctx, post.brandAccessories, holder.binding.layoutAccessories, holder.binding.lblAccessories)
        setupBrandView(ctx, post.occasion, holder.binding.layoutTarget, holder.binding.lbTarget)

        if (post.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .thumbnail(0.1f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_outfit)
                .into(holder.binding.imgMain)
        } else {
            holder.binding.imgMain.setImageResource(R.drawable.img_outfit)
        }

        val isLikedByMe = post.likedBy.contains(currentUserId)
        if (isLikedByMe) {
            holder.binding.btnLike.setIconResource(R.drawable.ic_heart_full)
            holder.binding.btnLike.setIconTintResource(R.color.red)
        } else {
            holder.binding.btnLike.setIconResource(R.drawable.ic_heart)
            holder.binding.btnLike.setIconTintResource(android.R.color.black)
        }

        holder.binding.btnLike.setOnClickListener {
            if (currentUserId == null) return@setOnClickListener
            onLikeClicked(post)
            val liked = post.likedBy.contains(currentUserId)
            val updated = post.likedBy.toMutableList()
            if (liked) {
                updated.remove(currentUserId)
                holder.binding.btnLike.setIconResource(R.drawable.ic_heart)
                holder.binding.btnLike.setIconTintResource(android.R.color.black)
            } else {
                updated.add(currentUserId!!)
                holder.binding.btnLike.setIconResource(R.drawable.ic_heart_full)
                holder.binding.btnLike.setIconTintResource(R.color.red)
            }
            post.likedBy = updated as ArrayList<String>
            holder.binding.lblLikeCount.text = post.likedBy.size.toString()
        }
    }

    override fun getItemCount() = posts.size

    fun updateList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    fun updateProfileImages(images: Map<String, String?>) {
        profileImages = images
        notifyDataSetChanged()
    }

    private fun setupBrandView(
        context: android.content.Context,
        brandInput: String,
        container: View,
        textView: android.widget.TextView
    ) {
        if (brandInput.isEmpty()) {
            container.visibility = View.GONE
            container.setOnClickListener(null)
        } else {
            container.visibility = View.VISIBLE
            if (BrandHelper.isUrl(brandInput)) {
                textView.text = BrandHelper.extractBrandName(brandInput)
                textView.paintFlags = textView.paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                container.setOnClickListener { BrandHelper.openUrl(context, brandInput) }
            } else {
                textView.text = brandInput
                textView.paintFlags = textView.paintFlags and android.graphics.Paint.UNDERLINE_TEXT_FLAG.inv()
                container.setOnClickListener(null)
            }
        }
    }
}
