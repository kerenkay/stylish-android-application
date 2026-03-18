package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentLikesBinding
import com.example.stylish_android_application.viewmodel.LikesViewModel

class LikesFragment : Fragment() {
    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!

    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var postsAdapter: ProfileAdapter
    private lateinit var viewModel: LikesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[LikesViewModel::class.java]

        setupAdapters()
        setupBackButton()
        setupObservers()

        // --- יירוט כפתור החזור כדי לסגור תיקיות פתוחות ---
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // בודקים האם אזור הפוסטים (התיקייה) פתוח וגלוי
                if (binding.rvLikedPosts.visibility == View.VISIBLE) {
                    // סוגרים את התיקייה וחוזרים לתצוגת התיקיות הראשית
                    closeFolder()
                } else {
                    // אם אנחנו במסך הראשי של השמירות, נבטל את היירוט שלנו
                    // וניתן למערכת לעשות את מה שמוגדר ב-MainActivity (לחזור לפיד)
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupAdapters() {
        // 1. Folders Adapter (2 columns)
        binding.rvFolders.layoutManager = GridLayoutManager(context, 2)
        foldersAdapter = FoldersAdapter(emptyList()) { selectedFolder ->
            openFolder(selectedFolder)
        }
        binding.rvFolders.adapter = foldersAdapter

        // 2. Posts Adapter inside a folder (3 columns)
        binding.rvLikedPosts.layoutManager = GridLayoutManager(context, 3)
        postsAdapter = ProfileAdapter(emptyList()) { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvLikedPosts.adapter = postsAdapter
    }

    // 3. Listen to data changes from the ViewModel
    private fun setupObservers() {
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            if (folders.isEmpty()) {
                binding.tvNoLikes.visibility = View.VISIBLE
                binding.rvFolders.visibility = View.GONE
            } else {
                binding.tvNoLikes.visibility = View.GONE
                binding.rvFolders.visibility = View.VISIBLE
                foldersAdapter.updateFolders(folders)
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            closeFolder()
        }
    }

    // --- UI Navigation (View States) ---

    private fun openFolder(folder: FolderItem) {
        binding.rvFolders.visibility = View.GONE
        binding.rvLikedPosts.visibility = View.VISIBLE

        binding.btnBack.visibility = View.VISIBLE
        binding.tvLikesTitle.text = folder.name
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 4.dpToPx(requireContext())
        }

        // Update the posts adapter using the function we added earlier!
        postsAdapter.updatePosts(folder.posts)
    }

    private fun closeFolder() {
        binding.rvLikedPosts.visibility = View.GONE
        binding.rvFolders.visibility = View.VISIBLE

        binding.btnBack.visibility = View.GONE
        binding.tvLikesTitle.text = "Saved Looks"
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 16.dpToPx(requireContext())
        }
    }

    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}