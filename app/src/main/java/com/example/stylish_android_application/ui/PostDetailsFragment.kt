package com.example.stylish_android_application.ui

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.stylish_android_application.utils.BrandHelper
import com.example.stylish_android_application.R
import com.example.stylish_android_application.databinding.FragmentPostDetailsBinding
import com.example.stylish_android_application.model.Post
import com.example.stylish_android_application.utils.showConfirmDialog
import com.example.stylish_android_application.viewmodel.PostDetailsState
import com.example.stylish_android_application.viewmodel.PostDetailsViewModel
import com.google.firebase.auth.FirebaseAuth

class PostDetailsFragment : Fragment() {

    private var _binding: FragmentPostDetailsBinding? = null
    private val binding get() = _binding!!
    private val currentUserId get() = FirebaseAuth.getInstance().currentUser?.uid

    private var currentPost: Post? = null
    private lateinit var viewModel: PostDetailsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PostDetailsViewModel::class.java]

        currentPost = arguments?.getSerializable("post") as? Post
        currentPost?.let { render(it) }

        parentFragmentManager.setFragmentResultListener("post_edited", viewLifecycleOwner) { _, bundle ->
            val updatedPost = bundle.getSerializable("post") as? Post
            updatedPost?.let {
                currentPost = it
                render(it)
            }
        }

        viewModel.authorProfile.observe(viewLifecycleOwner) { profile ->
            if (!profile.username.isNullOrEmpty()) {
                binding.fullPostCard.lblUser.text = profile.username
            }
            if (!profile.imageUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(profile.imageUrl)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.img_profile)
                    .into(binding.fullPostCard.imgProfile)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PostDetailsState.Deleted -> {
                    Toast.makeText(context, "Post deleted successfully", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                is PostDetailsState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    viewModel.resetState()
                }
                else -> {}
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val onUserClicked = View.OnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putString("USER_ID", currentPost?.userId)
            profileFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, profileFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.fullPostCard.imgProfile.setOnClickListener(onUserClicked)
        binding.fullPostCard.lblUser.setOnClickListener(onUserClicked)

        binding.fullPostCard.btnComment.setOnClickListener {
            val post = currentPost ?: return@setOnClickListener
            CommentsBottomSheet.newInstance(post.id)
                .show(childFragmentManager, "comments")
        }

        childFragmentManager.setFragmentResultListener("comment_count_changed", viewLifecycleOwner) { _, bundle ->
            val newCount = bundle.getLong("commentCount")
            currentPost?.commentCount = newCount
            binding.fullPostCard.lblCommentCount.text = newCount.toString()
        }
    }

    private fun render(post: Post) {
        setupUI(post)
        setupDeleteButton(post)
        setupEditButton(post)
        setupLikeButton(post)
    }

    private fun setupUI(post: Post) {
        val card = binding.fullPostCard
        card.lblUser.text = post.userName
        card.lblLikeCount.text = post.likedBy.size.toString()
        card.lblCommentCount.text = post.commentCount.toString()
        card.imgProfile.setImageResource(R.drawable.img_profile)

        viewModel.loadAuthorProfile(post.userId)

        if (post.description.isEmpty()) {
            card.lblDescription.visibility = View.GONE
        } else {
            card.lblDescription.visibility = View.VISIBLE
            card.lblDescription.text = post.description
        }

        setupBrandDetailView(post.brandTop, card.layoutTop, card.lblTop)
        setupBrandDetailView(post.brandBottom, card.layoutBottom, card.lblBottom)
        setupBrandDetailView(post.brandDress, card.layoutDress, card.lblDress)
        setupBrandDetailView(post.brandJacket, card.layoutJacket, card.lblJacket)
        setupBrandDetailView(post.brandShoes, card.layoutShoes, card.lblShoes)
        setupBrandDetailView(post.brandBag, card.layoutBag, card.lblBag)
        setupBrandDetailView(post.brandGlasses, card.layoutGlasses, card.lblGlasses)
        setupBrandDetailView(post.brandAccessories, card.layoutAccessories, card.lblAccessories)
        setupBrandDetailView(post.occasion, card.layoutTarget, card.lbTarget)

        if (post.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(post.imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.img_outfit)
                .into(card.imgMain)
        } else {
            card.imgMain.setImageResource(R.drawable.img_outfit)
        }
    }

    private fun setupEditButton(post: Post) {
        if (currentUserId == post.userId) {
            binding.btnEdit.visibility = View.VISIBLE
            binding.btnEdit.setOnClickListener {
                val editFragment = EditPostFragment()
                val bundle = Bundle()
                bundle.putSerializable("post", post)
                editFragment.arguments = bundle
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, editFragment)
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            binding.btnEdit.visibility = View.GONE
        }
    }

    private fun setupDeleteButton(post: Post) {
        if (currentUserId == post.userId) {
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener {
                showConfirmDialog(
                    context = requireContext(),
                    title = "Delete Post",
                    message = "Are you sure you want to delete this post?",
                    positiveLabel = "Delete",
                    onConfirm = { viewModel.deletePost(post) }
                )
            }
        } else {
            binding.btnDelete.visibility = View.GONE
        }
    }

    private fun setupLikeButton(post: Post) {
        updateLikeIcon(post)
        binding.fullPostCard.btnLike.setOnClickListener {
            val uid = currentUserId ?: return@setOnClickListener
            val isLiked = post.likedBy.contains(uid)
            if (isLiked) post.likedBy.remove(uid) else post.likedBy.add(uid)
            viewModel.toggleLike(post)
            updateLikeIcon(post)
            binding.fullPostCard.lblLikeCount.text = post.likedBy.size.toString()
        }
    }

    private fun updateLikeIcon(post: Post) {
        val isLiked = post.likedBy.contains(currentUserId)
        if (isLiked) {
            binding.fullPostCard.btnLike.setIconResource(R.drawable.ic_heart_full)
            binding.fullPostCard.btnLike.setIconTintResource(R.color.red)
        } else {
            binding.fullPostCard.btnLike.setIconResource(R.drawable.ic_heart)
            binding.fullPostCard.btnLike.setIconTintResource(android.R.color.black)
        }
    }

    private fun setupBrandDetailView(brandInput: String, container: View, textView: TextView) {
        if (brandInput.isEmpty()) {
            container.visibility = View.GONE
            container.setOnClickListener(null)
        } else {
            container.visibility = View.VISIBLE
            if (BrandHelper.isUrl(brandInput)) {
                textView.text = BrandHelper.extractBrandName(brandInput)
                textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                container.setOnClickListener { BrandHelper.openUrl(requireContext(), brandInput) }
            } else {
                textView.text = brandInput
                textView.paintFlags = textView.paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
                container.setOnClickListener(null)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}