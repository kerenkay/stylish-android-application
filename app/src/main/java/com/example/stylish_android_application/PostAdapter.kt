package com.example.stylish_android_application

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
// חשוב: נוודא שה-Binding מיובא
import com.example.stylish_android_application.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth

class PostsAdapter(
    private var posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onPostClicked: (Post) -> Unit
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

        // --- שינוי 3: מעכשיו ניגשים לכל השדות דרך holder.binding ---
        holder.binding.lblUser.text = post.userName
        holder.binding.lblDescription.text = post.description
        holder.binding.lbTarget.text = post.occasion
        holder.binding.lblLikeCount.text = post.likedBy.size.toString()
        setupBrandView(post.brandTop, holder.binding.imgTopIcon, holder.binding.lblTop)
        setupBrandView(post.brandBottom, holder.binding.imgBottomIcon, holder.binding.lblBottom)
        setupBrandView(post.brandJacket, holder.binding.imgJacketIcon, holder.binding.lblJacket)
        setupBrandView(post.brandShoes, holder.binding.imgShoesIcon, holder.binding.lblShoes)
        setupBrandView(post.brandBag, holder.binding.imgBagIcon, holder.binding.lblBag)
        setupBrandView(post.brandDress, holder.binding.imgDressIcon, holder.binding.lblDress)

        // --- תמונה ---
        if (post.imageUrl.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.binding.imgMain.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                holder.binding.imgMain.setImageResource(R.drawable.img_outfit)
            }
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

        holder.itemView.setOnClickListener {
            onPostClicked(post)
        }
    }

    override fun getItemCount() = posts.size

    fun updateList(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    private fun setupBrandView(brandName: String, iconView: View, textView: android.widget.TextView) {
        if (brandName.isEmpty()) {
            iconView.visibility = View.GONE
            textView.visibility = View.GONE
        } else {
            iconView.visibility = View.VISIBLE
            textView.visibility = View.VISIBLE
            textView.text = brandName
        }
    }
}