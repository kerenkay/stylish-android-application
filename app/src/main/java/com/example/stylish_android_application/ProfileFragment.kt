package com.example.stylish_android_application

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stylish_android_application.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val userPosts = mutableListOf<Post>()
    private lateinit var adapter: ProfileAdapter

    // משתנה לבחירת תמונה מהגלריה
    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadProfileImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. בודקים איזה משתמש צריך להציג (אם הגיע ID מהפיד ניקח אותו, אחרת ניקח את המשתמש המחובר)
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val targetUserId = arguments?.getString("USER_ID") ?: currentUserId ?: return

        // 2. בודקים אם זה הפרופיל שלי או של מישהו אחר
        val isCurrentUser = (currentUserId == targetUserId)

        setupUserProfile(targetUserId, isCurrentUser)
        setupRecyclerView()
        loadUserPosts(targetUserId) // שינינו את שם הפונקציה כדי שתקבל את ה-ID

        // 3. לוגיקה שרלוונטית רק אם זה הפרופיל שלי!
        if (isCurrentUser) {
            binding.btnLogout.visibility = View.VISIBLE

            // לחיצה על תמונת הפרופיל
            binding.imgProfile.setOnClickListener {
                pickProfileImage.launch("image/*")
            }

            binding.btnLogout.setOnClickListener {
                // מנתקים את המשתמש מ-Firebase
                FirebaseAuth.getInstance().signOut()
                // מעבירים אותו חזרה למסך ההתחברות
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        } else {
            // אם זה פרופיל של מישהו אחר: מסתירים כפתור התנתקות ומבטלים לחיצה על התמונה
            binding.btnLogout.visibility = View.GONE
            binding.imgProfile.setOnClickListener(null)
        }
    }

    private fun setupUserProfile(targetUserId: String, isCurrentUser: Boolean) {
        // 1. קודם כל ולפני הכל - אם זה הפרופיל שלי, נציג את השם מיד! (לא תלוי במסד הנתונים)
        if (isCurrentUser) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val name = currentUser?.displayName ?: currentUser?.email?.substringBefore("@") ?: "User"
            binding.tvUserName.text = name
        }

        // 2. מאזינים למסמך המשתמש כדי להביא את התמונה
        FirebaseFirestore.getInstance().collection("users").document(targetUserId)
            .addSnapshotListener { document, error ->
                if (_binding == null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    // אם זה משתמש אחר, ננסה לשלוף את השם מהמסמך שלו (אם שמרת שדה בשם username)
                    if (!isCurrentUser) {
                        val name = document.getString("username")
                        if (!name.isNullOrEmpty()) {
                            binding.tvUserName.text = name
                        }
                    }

                    // טעינת תמונת הפרופיל
                    // טעינת תמונת הפרופיל
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@ProfileFragment)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_launcher_background)
                            .circleCrop()
                            .into(binding.imgProfile)

                        // --- התוספת החדשה: לחיצה פותחת את התמונה בגדול! ---
                        binding.imgProfile.setOnClickListener {
                            showFullImageDialog(imageUrl)
                        }
                    } else {
                        binding.imgProfile.setImageResource(R.drawable.ic_launcher_background)
                        binding.imgProfile.setOnClickListener(null) // אם אין תמונה, אין מה להגדיל
                    }
                } else {
                    // אם למשתמש אין מסמך (למשל עוד לא העלה תמונה מעולם)
                    binding.imgProfile.setImageResource(R.drawable.img_profile)
                }
            }
    }

    private fun loadProfileImage(userId: String) {
        // שמים תמונת טעינה זמנית (אפשר להחליף לאייקון הפרופיל הריק שלך)
        binding.imgProfile.setImageResource(R.drawable.ic_launcher_background)

        FirebaseFirestore.getInstance().collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // שולפים את הלינק!
                    val imageUrl = document.getString("profileImageUrl")

                    // מוודאים שיש שם משהו
                    if (!imageUrl.isNullOrEmpty()) {
                        // נותנים ל-Glide לעשות את הקסם במהירות ולהציג את התמונה מעוגלת
                        Glide.with(requireContext())
                            .load(imageUrl)
                            .circleCrop()
                            .into(binding.imgProfile)
                    }
                }
            }
    }

    private fun uploadProfileImage(uri: android.net.Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. קוראים לפונקציה שלך שמכינה את התמונה (מקטינה ומסובבת נכון)
        val finalBitmap = getRotatedProfileBitmap(uri) ?: return

        // 2. הופכים אותה לפורמט שמתאים להעלאה (Bytes)
        val baos = ByteArrayOutputStream()
        finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        // 3. יוצרים "כתובת" במחסן של פיירבייס: תיקיית profile_images עם שם שהוא ה-ID של המשתמש
        val storageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")

        Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()

        // 4. מעלים את התמונה למחסן!
        storageRef.putBytes(data)
            .addOnSuccessListener {
                // 5. התמונה עלתה בהצלחה! עכשיו אנחנו מבקשים את הלינק הישיר אליה:
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->

                    // 6. שומרים *רק את הלינק* במסד הנתונים של המשתמש
                    val userData = hashMapOf("profileImageUrl" to downloadUrl.toString())

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(userData, SetOptions.merge()) // מרג' כדי לא למחוק את שם המשתמש!
                        .addOnSuccessListener {
                            binding.imgProfile.setImageBitmap(finalBitmap) // מציגים למשתמש
                            Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        binding.rvProfilePosts.layoutManager = GridLayoutManager(context, 3)

        adapter = ProfileAdapter(
            userPosts,
            onPostClick = { post ->
                val fragment = PostDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // ודאי שה-ID תואם ל-activity_main
                    .addToBackStack(null)
                    .commit()
            }
//            onPostLongClick = { post -> showDeleteDialog(post) }
        )
        binding.rvProfilePosts.adapter = adapter
    }

    private fun loadUserPosts(userId: String) {
        FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userId", userId) // שולף פוסטים לפי ה-ID שביקשנו!
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    userPosts.clear()
                    var totalLikes = 0

                    for (document in value) {
                        val post = document.toObject(Post::class.java)
                        post.id = document.id
                        userPosts.add(post)

                        // חישוב הלייקים
                        totalLikes += post.likedBy.size
                    }

                    // --- גיבוי חכם לשם המשתמש ---
                    // אם נכנסנו לפרופיל של מישהו אחר ויש לו פוסטים, ניקח את השם המדויק שלו משם!
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != currentUserId && userPosts.isNotEmpty()) {
                        binding.tvUserName.text = userPosts[0].userName
                    }

                    adapter.notifyDataSetChanged()

                    // עדכון התצוגה
                    binding.tvPostsCount.text = "${userPosts.size} Posts"
                    binding.tvTotalLikes.text = "$totalLikes Likes"
                }
            }
    }

    private fun showFullImageDialog(imageUrl: String) {
        // 1. יצירת דיאלוג (חלון צף) על מסך מלא עם רקע שחור
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        // 2. יצירת רכיב תמונה (ImageView) שיתפוס את כל המסך
        val imageView = android.widget.ImageView(requireContext())
        imageView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        // שומר על הפרופורציות של התמונה במרכז
        imageView.scaleType = android.widget.ImageView.ScaleType.FIT_CENTER

        // 3. טעינת התמונה החלקה מהזיכרון דרך Glide
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(imageView)

        dialog.setContentView(imageView)

        // 4. סגירת התמונה בלחיצה עליה (כדי שיהיה קל לחזור לפרופיל)
        imageView.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

//    private fun showDeleteDialog(post: Post) {
//        AlertDialog.Builder(context)
//            .setTitle("Change Profile Image")
//            .setMessage("Are you sure you want to change this image?")
//            .setPositiveButton("Change") { _, _ -> deletePost(post) }
//            .setNegativeButton("Cancel", null)
//            .show()
//    }
//
//    private fun deletePost(post: Post) {
//        FirebaseFirestore.getInstance().collection("posts")
//            .document(post.id)
//            .delete()
//            .addOnSuccessListener {
//                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
//                // אין צורך למחוק ידנית, ה-SnapshotListener יעשה את זה לבד
//            }
//    }

    private fun getRotatedProfileBitmap(uri: android.net.Uri): android.graphics.Bitmap? {
        // קריאת התמונה מהזיכרון
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        var bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        if (bitmap == null) return null

        // הקטנה משמעותית לפרופיל (300x300)
        val maxDimension = 300
        if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            bitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // סיבוב התמונה לפי המידע הנסתר (Exif)
        var rotatedBitmap = bitmap
        try {
            val exifInputStream = requireContext().contentResolver.openInputStream(uri)
            if (exifInputStream != null) {
                val exif = android.media.ExifInterface(exifInputStream)
                val orientation = exif.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, android.media.ExifInterface.ORIENTATION_NORMAL)

                val matrix = android.graphics.Matrix()
                when (orientation) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                if (orientation != android.media.ExifInterface.ORIENTATION_NORMAL && orientation != android.media.ExifInterface.ORIENTATION_UNDEFINED) {
                    rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                exifInputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return rotatedBitmap
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}