package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stylish_android_application.databinding.FragmentFeedBinding
import com.example.stylish_android_application.viewmodel.FeedViewModel

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostsAdapter

    // 1. Declare the ViewModel
    private lateinit var viewModel: FeedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Initialize the ViewModel
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            posts = emptyList(), // Start with an empty list, ViewModel will fill it!
            onLikeClicked = { post ->
                // Delegate business logic to ViewModel
                viewModel.toggleLike(post)
            },
            onUserClicked = { userId ->
                // Handle UI navigation inside the Fragment
                val profileFragment = ProfileFragment()
                val bundle = Bundle()
                bundle.putString("USER_ID", userId)
                profileFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit()
            }
        )
        val layoutManager = LinearLayoutManager(context)
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.adapter = adapter

        binding.rvPosts.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // בודקים אם אנחנו גוללים למטה
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    // אם התקרבנו לסוף הרשימה (נשארו פחות מ-3 פוסטים לראות)
                    if (!viewModel.isLastPage && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        viewModel.loadMorePosts()
                    }
                }
            }
        })
    }

    // 3. Listen to data changes from the ViewModel
    private fun setupObservers() {
        viewModel.postsList.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts) // Update the adapter efficiently
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}