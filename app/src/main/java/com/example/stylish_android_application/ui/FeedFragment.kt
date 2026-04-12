package com.example.stylish_android_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.stylish_android_application.ui.ProfileFragment
import com.example.stylish_android_application.R
import com.example.stylish_android_application.adapter.PostsAdapter
import com.example.stylish_android_application.databinding.FragmentFeedBinding
import com.example.stylish_android_application.viewmodel.FeedViewModel

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PostsAdapter

    private lateinit var viewModel: FeedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[FeedViewModel::class.java]
        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = PostsAdapter(
            posts = emptyList(),
            onLikeClicked = { post ->
                viewModel.toggleLike(post)
            },
            onUserClicked = { userId ->
                val profileFragment = ProfileFragment()
                val bundle = Bundle()
                bundle.putString("USER_ID", userId)
                profileFragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit()
            },
            onCommentClicked = { post ->
                CommentsBottomSheet.newInstance(post.id)
                    .show(childFragmentManager, "comments")
            }
        )

        childFragmentManager.setFragmentResultListener("comment_count_changed", viewLifecycleOwner) { _, bundle ->
            val postId = bundle.getString("postId") ?: return@setFragmentResultListener
            val newCount = bundle.getLong("commentCount")
            viewModel.updateCommentCount(postId, newCount)
        }
        val layoutManager = LinearLayoutManager(context)
        binding.rvPosts.layoutManager = LinearLayoutManager(context)
        binding.rvPosts.adapter = adapter

        binding.rvPosts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if (!viewModel.isLastPage && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                        viewModel.loadMorePosts()
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.refresh()
        }
    }

    //Listen to data changes from the ViewModel
    private fun setupObservers() {
        viewModel.postsList.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts)
        }
        viewModel.profileImages.observe(viewLifecycleOwner) { images ->
            adapter.updateProfileImages(images)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}