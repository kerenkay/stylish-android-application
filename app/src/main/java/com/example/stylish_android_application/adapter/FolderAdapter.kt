package com.example.stylish_android_application.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.model.Post
import com.example.stylish_android_application.R
import com.example.stylish_android_application.databinding.ItemFolderBinding

data class FolderItem(
    val name: String,
    val posts: List<Post>
)

class FoldersAdapter(
    private var folders: List<FolderItem>,
    private val onFolderClick: (FolderItem) -> Unit
) : RecyclerView.Adapter<FoldersAdapter.FolderViewHolder>() {

    class FolderViewHolder(val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = folders[position]

        holder.binding.tvFolderName.text = folder.name
        holder.binding.tvFolderCount.text = "${folder.posts.size} Saves"

        val coverPost = folder.posts.firstOrNull { it.imageUrl.isNotEmpty() }

        if (coverPost != null && coverPost.imageUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(coverPost.imageUrl)
                .override(400, 400)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_outfit)
                .into(holder.binding.imgFolderCover)
        } else {
            holder.binding.imgFolderCover.setImageResource(R.drawable.img_outfit)
        }

        holder.itemView.setOnClickListener { onFolderClick(folder) }
    }

    override fun getItemCount() = folders.size

    fun updateFolders(newFolders: List<FolderItem>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}