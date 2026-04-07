package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.stylish_android_application.databinding.FragmentEditPostBinding
import com.example.stylish_android_application.viewmodel.EditPostViewModel
import com.example.stylish_android_application.viewmodel.EditState

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditPostViewModel
    private lateinit var post: Post

    private val occasions = arrayOf("daily", "work", "brunch", "night out", "date", "wedding", "event", "other")

    private val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius", "reserved",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox", "Gali", "Fashion Club",
        "Carolina", "Blueberry", "Miss Nori", "Adika", "Yanga", "Cropp", "Aldo", "Black & white"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post = arguments?.getSerializable("post") as? Post ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        viewModel = ViewModelProvider(this)[EditPostViewModel::class.java]

        setupBrandAutocomplete()
        setupSpinner()
        populateFields()
        setupObservers()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.btnSave.setOnClickListener { saveChanges() }
    }

    private fun populateFields() {
        binding.etDescription.setText(post.description)
        binding.etBrandTop.setText(post.brandTop)
        binding.etBrandBottom.setText(post.brandBottom)
        binding.etJacket.setText(post.brandJacket)
        binding.etShoes.setText(post.brandShoes)
        binding.etBag.setText(post.brandBag)
        binding.etDress.setText(post.brandDress)
        binding.etGlasses.setText(post.brandGlasses)
        binding.etAccessories.setText(post.brandAccessories)

        // Set spinner to match post's occasion; fall back to "other" for custom values
        val index = occasions.indexOf(post.occasion)
        if (index >= 0) {
            binding.spinnerOccasion.setSelection(index)
        } else if (post.occasion.isNotEmpty()) {
            binding.spinnerOccasion.setSelection(occasions.indexOf("other"))
            binding.layoutOccasionOther.visibility = View.VISIBLE
            binding.etOccasionOther.setText(post.occasion)
        }
    }

    private fun setupBrandAutocomplete() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brandSuggestions)
        binding.etBrandTop.setAdapter(adapter)
        binding.etBrandBottom.setAdapter(adapter)
        binding.etJacket.setAdapter(adapter)
        binding.etShoes.setAdapter(adapter)
        binding.etBag.setAdapter(adapter)
        binding.etDress.setAdapter(adapter)
        binding.etGlasses.setAdapter(adapter)
        binding.etAccessories.setAdapter(adapter)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, occasions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.layoutOccasionOther.visibility =
                    if (occasions[position] == "other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun saveChanges() {
        val selectedOccasion = occasions[binding.spinnerOccasion.selectedItemPosition]
        val finalOccasion = if (selectedOccasion == "other") {
            val custom = binding.etOccasionOther.text.toString().trim()
            if (custom.isEmpty()) {
                Toast.makeText(context, "Please specify the occasion", Toast.LENGTH_SHORT).show()
                return
            }
            custom
        } else {
            selectedOccasion
        }

        viewModel.updatePost(
            postId = post.id,
            description = binding.etDescription.text.toString().trim(),
            brandTop = binding.etBrandTop.text.toString().trim(),
            brandBottom = binding.etBrandBottom.text.toString().trim(),
            brandJacket = binding.etJacket.text.toString().trim(),
            brandShoes = binding.etShoes.text.toString().trim(),
            brandBag = binding.etBag.text.toString().trim(),
            brandDress = binding.etDress.text.toString().trim(),
            brandGlasses = binding.etGlasses.text.toString().trim(),
            brandAccessories = binding.etAccessories.text.toString().trim(),
            occasion = finalOccasion
        )
    }

    private fun setupObservers() {
        viewModel.editState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditState.Loading -> binding.btnSave.isEnabled = false
                is EditState.Success -> {
                    Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
                is EditState.Error -> {
                    binding.btnSave.isEnabled = true
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }
                is EditState.Idle -> binding.btnSave.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
