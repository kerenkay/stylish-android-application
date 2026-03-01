package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentSearchBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    // אנחנו משתמשים באדפטר של הפרופיל (ProfileAdapter) במקום באדפטר הרגיל
    private lateinit var adapter: ProfileAdapter

    // שתי רשימות: אחת מחזיקה את כל הפוסטים תמיד, ואחת מחזיקה רק את מה שמוצג כרגע
    private val allPosts = mutableListOf<Post>()
    private val displayedPosts = mutableListOf<Post>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadAllPosts()
        setupSearchListener()
    }

    private fun setupRecyclerView() {
        // --- השינוי החשוב: תצוגת גריד של 3 עמודות ---
        binding.rvSearchResults.layoutManager = GridLayoutManager(context, 3)

        // יצירת האדפטר של הפרופיל
        adapter = ProfileAdapter(
            displayedPosts,
            onPostClick = { post ->
                // מעבר למסך פרטי הפוסט המלא בלחיצה
                val fragment = PostDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                fragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // ודאי שה-ID הזה תואם לאצלך
                    .addToBackStack(null)
                    .commit()
            },
            onPostLongClick = {
                // בלחיצה ארוכה מעמוד החיפוש לא נבצע מחיקה
                // (כי זה פוסטים של כולם, לא רק שלי). אפשר פשוט להשאיר ריק.
            }
        )
        binding.rvSearchResults.adapter = adapter
    }

    private fun loadAllPosts() {
        // שליפת *כל* הפוסטים (של כולם) מ-Firestore
        FirebaseFirestore.getInstance().collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                allPosts.clear()
                displayedPosts.clear()

                for (document in result) {
                    val post = document.toObject(Post::class.java)
                    post.id = document.id
                    allPosts.add(post)
                }

                // בהתחלה - מציגים את הכל
                displayedPosts.addAll(allPosts)
                adapter.notifyDataSetChanged()
            }
    }

    private fun setupSearchListener() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPosts(newText)
                return true
            }
        })
    }

    private fun filterPosts(query: String?) {
        displayedPosts.clear()

        // אם חיפוש ריק - נחזיר את כל הפוסטים
        if (query.isNullOrEmpty()) {
            displayedPosts.addAll(allPosts)
        } else {
            val lowerCaseQuery = query.lowercase(Locale.ROOT)

            // סינון לפי שם משתמש, מותגים, או אירוע
            val filteredList = allPosts.filter { post ->
                post.userName.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                        post.brandTop.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                        post.brandBottom.lowercase(Locale.ROOT).contains(lowerCaseQuery) ||
                        post.occasion.lowercase(Locale.ROOT).contains(lowerCaseQuery)
            }
            displayedPosts.addAll(filteredList)
        }

        // אם אין תוצאות - מציגים את ההודעה, אחרת מציגים את הגריד
        if (displayedPosts.isEmpty()) {
            binding.tvNoResults.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        } else {
            binding.tvNoResults.visibility = View.GONE
            binding.rvSearchResults.visibility = View.VISIBLE
        }

        adapter.notifyDataSetChanged() // עדכון המסך המיידי
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}