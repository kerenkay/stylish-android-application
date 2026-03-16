package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentLikesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LikesFragment : Fragment() {

    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!

    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var postsAdapter: ProfileAdapter

    private val allLikedPosts = mutableListOf<Post>()
    private val allFolders = mutableListOf<FolderItem>()
    private val currentFolderPosts = mutableListOf<Post>()
    private var snapshotListener: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAdapters()
        setupBackButton()
        loadLikedPosts()
    }

    private fun setupAdapters() {
        // 1. הגדרת אדפטר התיקיות (גריד של 2 עמודות)
        binding.rvFolders.layoutManager = GridLayoutManager(context, 2)
        foldersAdapter = FoldersAdapter(allFolders) { selectedFolder ->
            openFolder(selectedFolder)
        }
        binding.rvFolders.adapter = foldersAdapter

        // 2. הגדרת אדפטר הפוסטים (גריד של 3 עמודות, כמו בפרופיל)
        binding.rvLikedPosts.layoutManager = GridLayoutManager(context, 3)

        // כאן תיקנו את השגיאה: מחקנו את ה-onPostLongClick
        postsAdapter = ProfileAdapter(currentFolderPosts,
            onPostClick = { post ->
                val fragment = PostDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        binding.rvLikedPosts.adapter = postsAdapter
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            // לחיצה על חזור סוגרת את התיקייה ומראה שוב את כל התיקיות
            closeFolder()
        }
    }

    private fun loadLikedPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // 1. שומרים את ההאזנה לתוך המשתנה
        snapshotListener = FirebaseFirestore.getInstance().collection("posts")
            .whereArrayContains("likedBy", currentUserId)
            .addSnapshotListener { snapshot, error ->

                // 2. שורת ההגנה! אם המסך סגור, אל תעשה כלום ותעצור פה.
                if (_binding == null) return@addSnapshotListener

                if (snapshot != null) {
                    allLikedPosts.clear()
                    for (document in snapshot.documents) {
                        val post = document.toObject(Post::class.java)
                        if (post != null) {
                            post.id = document.id
                            allLikedPosts.add(post)
                        }
                    }
                    groupPostsIntoFolders()
                }
            }
    }

    private fun groupPostsIntoFolders() {
        allFolders.clear()

        if (allLikedPosts.isEmpty()) {
            binding.tvNoLikes.visibility = View.VISIBLE
            binding.rvFolders.visibility = View.GONE
            foldersAdapter.notifyDataSetChanged()
            return
        }

        binding.tvNoLikes.visibility = View.GONE
        binding.rvFolders.visibility = View.VISIBLE

        // יצירת תיקיית "All Saved" שתכיל את כל הפוסטים
        allFolders.add(FolderItem("All Saved", allLikedPosts))

        // קיבוץ הפוסטים לפי Occasion (המפתח זה ה-occasion, הערך זה רשימת הפוסטים)
        val groupedByOccasion = allLikedPosts.groupBy { it.occasion }

        for ((occasion, posts) in groupedByOccasion) {
            if (occasion.isNotEmpty()) {
                // הפיכת האות הראשונה לגדולה בשביל היופי
                val formattedName = occasion.replaceFirstChar { it.uppercase() }
                allFolders.add(FolderItem(formattedName, posts))
            }
        }

        foldersAdapter.notifyDataSetChanged()
    }

    // --- ניהול תצוגת מסכים ---

    private fun openFolder(folder: FolderItem) {
        // מסתירים את התיקיות, מראים את הפוסטים
        binding.rvFolders.visibility = View.GONE
        binding.rvLikedPosts.visibility = View.VISIBLE

        // מראים כפתור חזור ומשנים כותרת
        binding.btnBack.visibility = View.VISIBLE
        binding.tvLikesTitle.text = folder.name
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 4.dpToPx(requireContext())
        }
        // מעדכנים את הרשימה להציג רק את הפוסטים של התיקייה הזו
        currentFolderPosts.clear()
        currentFolderPosts.addAll(folder.posts)
        postsAdapter.notifyDataSetChanged()
    }

    fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun closeFolder() {
        // מסתירים את הפוסטים, מראים את התיקיות
        binding.rvLikedPosts.visibility = View.GONE
        binding.rvFolders.visibility = View.VISIBLE

        // מסתירים כפתור חזור ומחזירים כותרת מקורית
        binding.btnBack.visibility = View.GONE
        binding.tvLikesTitle.text = "Saved Looks"
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 16.dpToPx(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        snapshotListener?.remove()
        _binding = null
    }
}