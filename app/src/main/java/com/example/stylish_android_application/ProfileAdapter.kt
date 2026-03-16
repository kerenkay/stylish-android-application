package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.stylish_android_application.databinding.ItemProfileBinding

class ProfileAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    // --- ה-ViewHolder מקבל עכשיו את ה-Binding ---
    class ProfileViewHolder(val binding: ItemProfileBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        // --- מנפחים את ה-Binding ישירות מה-XML ---
        val binding = ItemProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProfileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val post = posts[position]

        if (post.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .override(400, 400) // הטריק: מכריח את Glide להתייחס לתמונה כקטנה במיוחד לגריד!
                .centerCrop()
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_outfit)
                .into(holder.binding.imgGridPost)
        } else {
            holder.binding.imgGridPost.setImageResource(R.drawable.img_outfit)
        }

        holder.binding.root.setOnClickListener {
            onPostClick(post)
        }
    }

    override fun getItemCount() = posts.size

}