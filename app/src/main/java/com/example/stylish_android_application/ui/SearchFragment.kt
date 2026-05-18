package com.example.stylish_android_application.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.R
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.ui.WeatherFragment
import com.example.stylish_android_application.adapter.ProfileAdapter
import com.example.stylish_android_application.databinding.FragmentSearchBinding
import com.example.stylish_android_application.viewmodel.SearchViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProfileAdapter
    private lateinit var viewModel: SearchViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSearchListener()
        setupBackButtonLogic()

        val searchEditText = binding.searchView.findViewById<EditText>(R.id.search_src_text)
        searchEditText.textSize = 12f

        binding.btnWeatherMatch.setOnClickListener {
            val weatherFragment = WeatherFragment()
            parentFragmentManager.beginTransaction()
                .replace(com.example.stylish_android_application.R.id.fragment_container, weatherFragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::viewModel.isInitialized) {
            viewModel.reload()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResults.layoutManager = GridLayoutManager(context, 3)
        adapter = ProfileAdapter(emptyList()) { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(com.example.stylish_android_application.R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvSearchResults.adapter = adapter
    }

    private fun setupObservers() {
        viewModel.displayedPosts.observe(viewLifecycleOwner) { posts ->
            if (posts.isEmpty()) {
                binding.tvNoResults.visibility = View.VISIBLE
                binding.rvSearchResults.visibility = View.GONE
            } else {
                binding.tvNoResults.visibility = View.GONE
                binding.rvSearchResults.visibility = View.VISIBLE
                adapter.updatePosts(posts)
            }
        }
    }

    private fun setupSearchListener() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.filterPosts(newText)
                return true
            }
        })
    }

    private fun setupBackButtonLogic() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentQuery = binding.searchView.query.toString()

                if (currentQuery.isNotEmpty()) {
                    binding.searchView.setQuery("", false)
                    binding.searchView.clearFocus()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}