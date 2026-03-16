package com.example.stylish_android_application

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.databinding.ItemFolderBinding

// מחלקה פשוטה שמייצגת תיקייה
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

        // עדכון הטקסטים
        holder.binding.tvFolderName.text = folder.name
        holder.binding.tvFolderCount.text = "${folder.posts.size} Saves"

        // טעינת תמונת השער (הפוסט הראשון בתיקייה שיש לו תמונה)
        val coverPost = folder.posts.firstOrNull { it.imageUrl.isNotEmpty() }

        if (coverPost != null && coverPost.imageUrl.isNotEmpty()) {
            // טעינה מהירה וחכמה עם Glide
            Glide.with(holder.itemView.context)
                .load(coverPost.imageUrl)
                .override(400, 400)
                .centerCrop() // חותך את התמונה יפה שתמלא את ריבוע התיקייה
                .diskCacheStrategy(DiskCacheStrategy.ALL) // קורא מהזיכרון ששמרנו קודם!
                .placeholder(R.drawable.img_outfit)
                .into(holder.binding.imgFolderCover)
        } else {
            holder.binding.imgFolderCover.setImageResource(R.drawable.img_outfit) // תמונת ברירת מחדל
        }

        holder.itemView.setOnClickListener { onFolderClick(folder) }
    }

    override fun getItemCount() = folders.size

    fun updateFolders(newFolders: List<FolderItem>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}