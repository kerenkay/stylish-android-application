package com.example.stylish_android_application

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stylish_android_application.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth

class PostsAdapter(
    private var posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onUserClicked: (String) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // --- שינוי 1: ה-ViewHolder מקבל עכשיו את ה-Binding במקום View רגיל ---
    // שימי לב שמחקנו מפה את כל ה-findViewById!
    class PostViewHolder(val binding: ItemPostBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        // --- שינוי 2: מנפחים את ה-Binding במקום ליצור View ---
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.binding.lblUser.text = post.userName
        holder.binding.lblLikeCount.text = post.likedBy.size.toString()

        if (post.description.isEmpty()) {
            holder.binding.lblDescription.visibility = View.GONE
        } else {
            holder.binding.lblDescription.visibility = View.VISIBLE
            holder.binding.lblDescription.text = post.description
        }

        setupBrandView(post.brandTop, holder.binding.layoutTop, holder.binding.lblTop)
        setupBrandView(post.brandBottom, holder.binding.layoutBottom, holder.binding.lblBottom)
        setupBrandView(post.brandDress, holder.binding.layoutDress, holder.binding.lblDress)
        setupBrandView(post.brandJacket, holder.binding.layoutJacket, holder.binding.lblJacket)
        setupBrandView(post.brandShoes, holder.binding.layoutShoes, holder.binding.lblShoes)
        setupBrandView(post.brandBag, holder.binding.layoutBag, holder.binding.lblBag)
        setupBrandView(post.occasion, holder.binding.layoutTarget, holder.binding.lbTarget)

        // --- תמונה ---
        // --- תמונה מהירה דרך Glide ---
        if (post.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .thumbnail(0.1f) // הטריק: טוען מיד גרסה מטושטשת ב-10% איכות למניעת מסך ריק!
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL) // שומר לזיכרון
                .placeholder(R.drawable.img_outfit)
                .into(holder.binding.imgMain)
        } else {
            holder.binding.imgMain.setImageResource(R.drawable.img_outfit)
        }

        // --- לוגיקה ללייקים ---
        val isLikedByMe = post.likedBy.contains(currentUserId)
        if (isLikedByMe) {
            holder.binding.btnLike.setIconResource(R.drawable.ic_heart_full)
            holder.binding.btnLike.setIconTintResource(R.color.red)
        } else {
            holder.binding.btnLike.setIconResource(R.drawable.ic_heart)
            holder.binding.btnLike.setIconTintResource(android.R.color.black)
        }

        // --- לחיצות ---
        holder.binding.btnLike.setOnClickListener {
            onLikeClicked(post)
            holder.binding.lblLikeCount.text = post.likedBy.size.toString()
        }

//        holder.itemView.setOnClickListener {
//            onPostClicked(post)
//        }

        holder.binding.lblUser.setOnClickListener {
            // קוראים לפונקציה שמעבירה את ה-ID החוצה
            onUserClicked(post.userId)
        }

//        // (מומלץ להוסיף את אותה לחיצה גם על תמונת הפרופיל שלו בפוסט)
//        holder.binding.imgProfileSmall.setOnClickListener {
//            onUserClicked(post.userId)
//        }
    }

    override fun getItemCount() = posts.size

    fun updateList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    private fun setupBrandView(brandName: String, container: View, textView: android.widget.TextView) {
        if (brandName.isEmpty()) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            textView.text = brandName
        }
    }
}