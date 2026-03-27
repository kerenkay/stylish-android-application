package com.example.stylish_android_application

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.databinding.ItemUserBinding
import com.example.stylish_android_application.viewmodel.FollowListViewModel
import com.google.firebase.auth.FirebaseAuth

class UserAdapter(
    private var users: List<FollowListViewModel.UserItem>,
    private val onUserClick: (String) -> Unit,
    private val onFollowClick: (String) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    inner class UserViewHolder(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        val binding = holder.binding

        binding.tvUserName.text = user.name

        if (user.profileImageUrl.isNotEmpty()) {
            Glide.with(binding.imgUserAvatar.context)
                .load(user.profileImageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .placeholder(R.drawable.img_profile)
                .into(binding.imgUserAvatar)
        } else {
            binding.imgUserAvatar.setImageResource(R.drawable.img_profile)
        }

        // Hide follow button for the current user's own row
        if (user.id == currentUserId) {
            binding.btnFollow.visibility = View.GONE
        } else {
            binding.btnFollow.visibility = View.VISIBLE
            if (user.isFollowing) {
                binding.btnFollow.text = "Following"
                binding.btnFollow.setBackgroundColor(Color.parseColor("#787770"))
            } else {
                binding.btnFollow.text = "Follow"
                binding.btnFollow.setBackgroundColor(Color.parseColor("#222222"))
            }
            binding.btnFollow.setOnClickListener { onFollowClick(user.id) }
        }

        holder.itemView.setOnClickListener { onUserClick(user.id) }
    }

    override fun getItemCount() = users.size

    fun updateList(newUsers: List<FollowListViewModel.UserItem>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
