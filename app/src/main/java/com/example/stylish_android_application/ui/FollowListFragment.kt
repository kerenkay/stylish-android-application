package com.example.stylish_android_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stylish_android_application.ui.ProfileFragment
import com.example.stylish_android_application.R
import com.example.stylish_android_application.adapter.UserAdapter
import com.example.stylish_android_application.databinding.FragmentFollowListBinding
import com.example.stylish_android_application.viewmodel.FollowListViewModel

class FollowListFragment : Fragment() {

    private var _binding: FragmentFollowListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: FollowListViewModel
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFollowListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val targetUserId = arguments?.getString("USER_ID") ?: return
        val mode = arguments?.getString("MODE") ?: "FOLLOWERS"

        viewModel = ViewModelProvider(this)[FollowListViewModel::class.java]

        binding.tvTitle.text = if (mode == "FOLLOWERS") "Followers" else "Following"

        setupRecyclerView()
        setupObservers()
        setupSearch()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewModel.loadUsers(targetUserId, mode)
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            emptyList(),
            onUserClick = { userId ->
                val fragment = ProfileFragment()
                fragment.arguments = Bundle().apply { putString("USER_ID", userId) }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onFollowClick = { userId ->
                viewModel.toggleFollow(userId)
            }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.filteredUsers.observe(viewLifecycleOwner) { users ->
            adapter.updateList(users)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterUsers(newText ?: "")
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}