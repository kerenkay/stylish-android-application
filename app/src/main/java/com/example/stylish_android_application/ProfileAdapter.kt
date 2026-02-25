package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ProfileAdapter(
    private val posts: List<Post>,
    private val onPostClick: (Post) -> Unit,
    private val onPostLongClick: (Post) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ProfileViewHolder>() {

    class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.imgGridPost)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileViewHolder {
        // ודאי ששם ה-XML כאן תואם לקובץ שיצרת (item_profile או item_profile_post)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_profile, parent, false)
        return ProfileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileViewHolder, position: Int) {
        val post = posts[position]

        if (post.imageUrl.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)

                // --- התיקון: טעינה חכמה וחסכונית בזיכרון ---
                // אנו מבקשים תמונה בגודל 200x200 בערך, שזה מספיק לגריד
                val decodedBitmap = decodeSampledBitmapFromByteArray(decodedBytes, 200, 200)

                holder.image.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                holder.image.setImageResource(R.drawable.img_outfit)
            }
        }

        holder.itemView.setOnClickListener { onPostClick(post) }
        holder.itemView.setOnLongClickListener {
            onPostLongClick(post)
            true
        }
    }

    override fun getItemCount() = posts.size

    // --- פונקציות עזר למניעת קריסה (OutOfMemory) ---

    // 1. פונקציה שמחשבת פי כמה להקטין את התמונה
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

    // 2. פונקציה שטוענת את התמונה בגודל המוקטן
    private fun decodeSampledBitmapFromByteArray(data: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap {
        // שלב א: בודקים מה הגודל המקורי של התמונה בלי לטעון אותה לזיכרון
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, this)

            // שלב ב: חישוב יחס ההקטנה
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // שלב ג: טעינת התמונה האמיתית כשהיא מוקטנת
            inJustDecodeBounds = false
            BitmapFactory.decodeByteArray(data, 0, data.size, this)
        }
    }
}