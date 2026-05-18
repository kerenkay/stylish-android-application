package com.example.stylish_android_application.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.ui.PostDetailsFragment
import com.example.stylish_android_application.R
import com.example.stylish_android_application.adapter.FolderItem
import com.example.stylish_android_application.adapter.FoldersAdapter
import com.example.stylish_android_application.adapter.ProfileAdapter
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
        setupBackButtonLogic()
    }

    private fun setupAdapters() {
        binding.rvFolders.layoutManager = GridLayoutManager(context, 2)
        foldersAdapter = FoldersAdapter(emptyList()) { selectedFolder ->
            openFolderUI(selectedFolder)
        }
        binding.rvFolders.adapter = foldersAdapter

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

    private fun setupObservers() {
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            if (folders.isEmpty()) {
                binding.tvNoLikes.visibility = View.VISIBLE
                binding.rvFolders.visibility = View.GONE
                binding.rvLikedPosts.visibility = View.GONE
                binding.btnBack.visibility = View.GONE
            } else {
                binding.tvNoLikes.visibility = View.GONE
                foldersAdapter.updateFolders(folders)

                val openedName = viewModel.openedFolderName
                if (openedName != null) {
                    val folderToOpen = folders.find { it.name == openedName }
                    if (folderToOpen != null) {
                        openFolderUI(folderToOpen)
                    } else {
                        closeFolderUI()
                    }
                } else {
                    closeFolderUI()
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            closeFolderUI()
        }
    }

    private fun setupBackButtonLogic() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.openedFolderName != null) {
                    closeFolderUI()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }


    private fun openFolderUI(folder: FolderItem) {
        viewModel.openedFolderName = folder.name

        binding.rvFolders.visibility = View.GONE
        binding.rvLikedPosts.visibility = View.VISIBLE

        binding.btnBack.visibility = View.VISIBLE
        binding.tvLikesTitle.text = folder.name
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 4.dpToPx(requireContext())
        }

        postsAdapter.updatePosts(folder.posts)
    }

    private fun closeFolderUI() {
        viewModel.openedFolderName = null

        binding.rvLikedPosts.visibility = View.GONE
        binding.rvFolders.visibility = View.VISIBLE

        binding.btnBack.visibility = View.GONE
        binding.tvLikesTitle.text = "Saved Looks"
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 16.dpToPx(requireContext())
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}