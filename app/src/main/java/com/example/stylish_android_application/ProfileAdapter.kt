package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.stylish_android_application.databinding.ItemProfileBinding

class ProfileAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit,
    private val onPostLongClick: (Post) -> Unit
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
            try {
                val decodedBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)

                // טעינה חכמה וחסכונית בזיכרון (כמו שכבר עשינו)
                val decodedBitmap = decodeSampledBitmapFromByteArray(decodedBytes, 200, 200)

                // --- גישה לתמונה דרך ה-Binding ---
                holder.binding.imgGridPost.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                holder.binding.imgGridPost.setImageResource(R.drawable.img_outfit)
            }
        }

        // --- לחיצות ---
        holder.binding.root.setOnClickListener { onPostClick(post) }
        holder.binding.root.setOnLongClickListener {
            onPostLongClick(post)
            true
        }
    }

    override fun getItemCount() = posts.size

    // --- פונקציות עזר למניעת קריסה (OutOfMemory) - נשארות כמו שהן ---

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