package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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
        if (coverPost != null) {
            try {
                val decodedBytes = Base64.decode(coverPost.imageUrl, Base64.DEFAULT)
                val decodedBitmap = decodeSampledBitmapFromByteArray(decodedBytes, 300, 300)
                holder.binding.imgFolderCover.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                holder.binding.imgFolderCover.setImageResource(R.drawable.img_outfit)
            }
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

    // פונקציות העזר החסכוניות בזיכרון
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, this)
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, 0, data.size, this)
        }
    }
}