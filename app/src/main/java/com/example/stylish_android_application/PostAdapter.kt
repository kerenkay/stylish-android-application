package com.example.stylish_android_application

import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

// האדפטר מקבל עכשיו גם "Callback" - פונקציה שתופעל כשלוחצים על לייק
class PostsAdapter(
    private val posts: List<Post>,
    private val onLikeClicked: (Post) -> Unit // פונקציה חיצונית לטיפול בלייק
) : RecyclerView.Adapter<PostsAdapter.PostViewHolder>() {

    // מזהה המשתמש הנוכחי (כדי שנדע אם לצבוע את הלב או לא)
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgMain: ImageView = itemView.findViewById(R.id.imgMain)
        val lblUser: TextView = itemView.findViewById(R.id.lblUser)
        val lblDescription: TextView = itemView.findViewById(R.id.lblDescription)
        val lblTop: TextView = itemView.findViewById(R.id.lblTop)
        val lblBottom: TextView = itemView.findViewById(R.id.lblBottom)
        val lbTarget: TextView = itemView.findViewById(R.id.lbTarget)
        val btnLike: MaterialButton = itemView.findViewById(R.id.btnLike)

        val lblLikeCount: TextView = itemView.findViewById(R.id.lblLikeCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]

        holder.lblUser.text = post.userName
        holder.lblDescription.text = post.description
        holder.lblTop.text = post.brandTop
        holder.lblBottom.text = post.brandBottom
        holder.lbTarget.text = post.occasion
        holder.lblLikeCount.text = post.likedBy.size.toString()


        // --- טיפול בתמונה ---
        if (post.imageUrl.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(post.imageUrl, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.imgMain.setImageBitmap(decodedBitmap)
            } catch (e: Exception) {
                holder.imgMain.setImageResource(R.drawable.img_outfit)
            }
        }

        // --- טיפול בלייקים (החלק החדש) ---

        // 1. בדיקה: האם המשתמש שלי נמצא ברשימת הלייקים של הפוסט?
        val isLikedByMe = post.likedBy.contains(currentUserId)

        // 2. עיצוב הכפתור בהתאם
        if (isLikedByMe) {
            holder.btnLike.setIconResource(R.drawable.ic_heart_full) // לב מלא (אדום)
            holder.btnLike.setIconTintResource(R.color.red) // צובע באדום (אם יש לך צבע כזה)
        } else {
            holder.btnLike.setIconResource(R.drawable.ic_heart) // לב ריק
            holder.btnLike.setIconTintResource(android.R.color.black) // צובע בשחור
        }

        // 3. לחיצה על הכפתור
        holder.btnLike.setOnClickListener {
            onLikeClicked(post) // מפעיל את הפונקציה בפרגמנט
            holder.lblLikeCount.text = post.likedBy.size.toString()
        }
    }

    override fun getItemCount() = posts.size
}