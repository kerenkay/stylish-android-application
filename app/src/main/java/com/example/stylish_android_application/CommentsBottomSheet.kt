package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stylish_android_application.databinding.FragmentCommentsBottomSheetBinding
import com.example.stylish_android_application.viewmodel.CommentsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentsBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme() = R.style.StylishBottomSheetDialog

    private var _binding: FragmentCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CommentsViewModel
    private lateinit var adapter: CommentAdapter
    private lateinit var postId: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString("POST_ID") ?: return

        viewModel = ViewModelProvider(this)[CommentsViewModel::class.java]

        adapter = CommentAdapter(emptyList()) { comment ->
            viewModel.deleteComment(postId, comment.commentId)
        }

        binding.rvComments.layoutManager = LinearLayoutManager(context)
        binding.rvComments.adapter = adapter

        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            adapter.updateComments(comments)
            binding.rvComments.scrollToPosition(comments.size - 1)
        }

        viewModel.loadComments(postId)

        binding.btnSendComment.setOnClickListener {
            val text = binding.etComment.text?.toString() ?: return@setOnClickListener
            if (text.isBlank()) return@setOnClickListener
            viewModel.addComment(postId, text)
            binding.etComment.setText("")
        }
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        val count = viewModel.commentCount.value ?: 0
        parentFragmentManager.setFragmentResult(
            "comment_count_changed",
            bundleOf("postId" to postId, "commentCount" to count)
        )
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(postId: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = bundleOf("POST_ID" to postId)
            }
        }
    }
}
