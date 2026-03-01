package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stylish_android_application.databinding.FragmentFeedBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val postsList = mutableListOf<Post>()
    private lateinit var adapter: PostsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadPosts()
    }

    private fun setupRecyclerView() {
        // כאן אנחנו מעבירים לאדפטר את הפונקציה המטפלת בלייק
        adapter = PostsAdapter(
            postsList,
            onLikeClicked = { post -> toggleLike(post) },
            onPostClicked = { post ->
                // אותו קוד מעבר כמו בחיפוש
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
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.adapter = adapter
    }

    // --- הפונקציה שמבצעת את הלייק מול Firebase ---
    private fun toggleLike(post: Post) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val db = FirebaseFirestore.getInstance()
        val postRef = db.collection("posts").document(post.id) // חייב שיהיה לנו ID לפוסט!

        val isLiked = post.likedBy.contains(currentUser.uid)

        if (isLiked) {
            // אם כבר עשיתי לייק -> תמחק אותי מהרשימה (Unlike)
            postRef.update("likedBy", FieldValue.arrayRemove(currentUser.uid))
        } else {
            // אם לא עשיתי לייק -> תוסיף אותי לרשימה (Like)
            postRef.update("likedBy", FieldValue.arrayUnion(currentUser.uid))
        }
    }

    private fun loadPosts() {
        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (value != null) {
                    postsList.clear()
                    for (document in value) {
                        val post = document.toObject(Post::class.java)

                        // *** חשוב מאוד: שומרים את ה-ID של המסמך בתוך האובייקט ***
                        post.id = document.id

                        postsList.add(post)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}