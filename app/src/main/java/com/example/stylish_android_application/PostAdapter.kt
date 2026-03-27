package com.example.stylish_android_application

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.databinding.ItemPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PostsAdapter(
    private var posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit,
    private val onUserClicked: (String) -> Unit
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    // Cache: userId → profileImageUrl (null means "fetched but no image")
    private val profileImageCache = HashMap<String, String?>()

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

        // Profile image: reset to default, then load from cache or Firestore
        holder.binding.imgProfile.setImageResource(R.drawable.img_profile)
        holder.binding.imgProfile.tag = post.userId

        if (profileImageCache.containsKey(post.userId)) {
            val url = profileImageCache[post.userId]
            if (!url.isNullOrEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(url)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.img_profile)
                    .into(holder.binding.imgProfile)
            }
        } else {
            FirebaseFirestore.getInstance().collection("users").document(post.userId)
                .get()
                .addOnSuccessListener { doc ->
                    val url = doc.getString("profileImageUrl")
                    profileImageCache[post.userId] = url
                    if (!url.isNullOrEmpty() && holder.binding.imgProfile.tag == post.userId) {
                        Glide.with(holder.itemView.context)
                            .load(url)
                            .circleCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .placeholder(R.drawable.img_profile)
                            .into(holder.binding.imgProfile)
                    }
                }
        }

        holder.binding.imgProfile.setOnClickListener {
            onUserClicked(post.userId)
        }

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
        setupBrandView(post.brandGlasses, holder.binding.layoutGlasses, holder.binding.lblGlasses)
        setupBrandView(post.brandAccessories, holder.binding.layoutAccessories, holder.binding.lblAccessories)
        setupBrandView(post.occasion, holder.binding.layoutTarget, holder.binding.lbTarget)

        // --- תמונה ---
        // --- תמונה מהירה דרך Glide ---
        if (post.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(post.imageUrl)
                .thumbnail(0.1f)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
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
            val isLikedByMe = post.likedBy.contains(currentUserId)
            val updatedLikedBy = post.likedBy.toMutableList()

            if (isLikedByMe) {
                updatedLikedBy.remove(currentUserId)
                holder.binding.btnLike.setIconResource(R.drawable.ic_heart)
                holder.binding.btnLike.setIconTintResource(android.R.color.black)
            } else {
                updatedLikedBy.add(currentUserId)
                holder.binding.btnLike.setIconResource(R.drawable.ic_heart_full)
                holder.binding.btnLike.setIconTintResource(R.color.red) // ודאי שיש לך צבע כזה ב-res/values/colors.xml
            }

            post.likedBy = updatedLikedBy as ArrayList<String>
            holder.binding.lblLikeCount.text = post.likedBy.size.toString()
        }

//        holder.itemView.setOnClickListener {
//            onPostClicked(post)
//        }

        holder.binding.lblUser.setOnClickListener {
            onUserClicked(post.userId)
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

    private fun setupBrandView(brandName: String, container: View, textView: android.widget.TextView) {
        if (brandName.isEmpty()) {
            container.visibility = View.GONE
        } else {
            container.visibility = View.VISIBLE
            textView.text = brandName
        }
    }
}