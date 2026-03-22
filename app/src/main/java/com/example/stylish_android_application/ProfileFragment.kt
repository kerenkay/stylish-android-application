package com.example.stylish_android_application

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.stylish_android_application.databinding.FragmentProfileBinding
import com.example.stylish_android_application.utils.ImageUtils
import com.example.stylish_android_application.viewmodel.ProfileViewModel
import com.example.stylish_android_application.viewmodel.UploadState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    // 1. Declare the ViewModel
    private lateinit var viewModel: ProfileViewModel
    private lateinit var adapter: ProfileAdapter

    // Launcher for picking an image from the gallery
    private val pickProfileImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            processAndUploadImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. Initialize the ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Determine which user profile to show
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val targetUserId = arguments?.getString("USER_ID") ?: currentUserId ?: return
        val isCurrentUser = (currentUserId == targetUserId)

        setupUI(targetUserId,isCurrentUser)
        setupRecyclerView()
        setupObservers()

        // 3. Tell the ViewModel to start fetching data!
        viewModel.loadUserProfile(targetUserId, isCurrentUser)
        viewModel.loadUserPosts(targetUserId)
    }

    // --- UI Setup Methods ---

    private fun setupUI(targetUserId: String,isCurrentUser: Boolean) {
        if (isCurrentUser) {
            binding.btnLogout.visibility = View.VISIBLE
            binding.btnFollow.visibility = View.GONE

            // Long click to change profile picture
            binding.imgProfile.setOnLongClickListener {
                showChangeProfileImageDialog()
                true
            }

            // Click to logout with confirmation
            binding.btnLogout.setOnClickListener {
                showLogoutConfirmationDialog()
            }
        } else {
            // Foreign profile: hide logout and disable long click
            binding.btnLogout.visibility = View.GONE
            binding.btnFollow.visibility = View.VISIBLE
            binding.imgProfile.setOnLongClickListener(null)
            binding.btnFollow.setOnClickListener {
                viewModel.toggleFollow(targetUserId)
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvProfilePosts.layoutManager = GridLayoutManager(context, 3)
        // Pass an empty list initially; the observer will update it
        adapter = ProfileAdapter(emptyList()) { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvProfilePosts.adapter = adapter
    }

    // --- Architecture Magic: Observing the ViewModel ---

    private fun setupObservers() {
        // Observe User Name
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        // Observe Profile Image URL
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(binding.imgProfile)

                // Click opens the full screen image dialog
                binding.imgProfile.setOnClickListener {
                    showFullImageDialog(imageUrl)
                }
            } else {
                binding.imgProfile.setImageResource(R.drawable.img_outfit) // Default profile image
                binding.imgProfile.setOnClickListener(null)
            }
        }

        // Observe User Posts
        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            adapter.updatePosts(posts) // Make sure your ProfileAdapter has an 'updatePosts' method!
            binding.tvPostsCount.text = "${posts.size} \nPosts"
        }

        // Observe Total Likes
        viewModel.totalLikes.observe(viewLifecycleOwner) { likes ->
            binding.tvTotalLikes.text = "$likes \nLikes"
        }

        // Observe Followers
        viewModel.followersCount.observe(viewLifecycleOwner) { count ->
            binding.tvFollowers.text = "$count \nFollowers"
        }
        viewModel.followingCount.observe(viewLifecycleOwner) { count ->
            binding.tvFollowing.text = "$count \nFollowing"
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            if (isFollowing) {
                binding.btnFollow.text = "Unfollow"
                binding.btnFollow.setBackgroundColor(Color.parseColor("#787770"))
            } else {
                binding.btnFollow.text = "Follow"
                binding.btnFollow.setBackgroundColor(Color.parseColor("#222222"))
            }
        }

        // Observe Upload State for profile image updates
        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UploadState.Loading -> {
                    Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                }
                is UploadState.Success -> {
                    Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    viewModel.resetUploadState()
                }
                is UploadState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_SHORT).show()
                    viewModel.resetUploadState()
                }
                is UploadState.Idle -> { /* Do nothing */ }
            }
        }
    }

    // --- Actions ---

    private fun processAndUploadImage(uri: Uri) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Process image off the main thread to prevent UI freezing
        lifecycleScope.launch(Dispatchers.Default) {
            val imagePair = ImageUtils.processProfileImage(requireContext(), uri)

            withContext(Dispatchers.Main) {
                if (imagePair != null) {
                    val (bitmap, imageBytes) = imagePair
                    // Show immediate local feedback to the user
                    binding.imgProfile.setImageBitmap(bitmap)
                    // Tell ViewModel to handle the actual upload
                    viewModel.uploadProfileImage(uid, imageBytes)
                } else {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- Dialogs ---

    private fun showFullImageDialog(imageUrl: String) {
        val dialog = android.app.Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = android.widget.ImageView(requireContext()).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
        }

        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(imageView)

        dialog.setContentView(imageView)
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun showChangeProfileImageDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Change Profile Picture")
            .setMessage("Would you like to update your profile picture?")
            .setPositiveButton("Change") { _, _ ->
                pickProfileImage.launch("image/*")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out of STYLISH?")
            .setPositiveButton("Log Out") { _, _ ->
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}