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
    private var pendingPost: Post? = null

    private val brandFormHelper = BrandFormHelper(this, emptyFieldMeansBlank = true)

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

        brandFormHelper.setupBrandAutocomplete(
            binding.etBrandTop, binding.etBrandBottom, binding.etJacket, binding.etShoes,
            binding.etBag, binding.etDress, binding.etGlasses, binding.etAccessories
        )
        brandFormHelper.setupBrandLinkIcons(listOf(
            binding.etBrandTop to binding.layoutBrandTop,
            binding.etBrandBottom to binding.layoutBrandBottom,
            binding.etJacket to binding.layoutBrandJacket,
            binding.etShoes to binding.layoutBrandShoes,
            binding.etBag to binding.layoutBrandBag,
            binding.etDress to binding.layoutBrandDress,
            binding.etGlasses to binding.layoutBrandGlasses,
            binding.etAccessories to binding.layoutBrandAccessories
        ))
        setupSpinner()
        populateFields()
        setupObservers()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { saveChanges() }
    }

    private fun populateFields() {
        binding.etDescription.setText(post.description)

        brandFormHelper.setBrandField(binding.etBrandTop, binding.layoutBrandTop, post.brandTop)
        brandFormHelper.setBrandField(binding.etBrandBottom, binding.layoutBrandBottom, post.brandBottom)
        brandFormHelper.setBrandField(binding.etJacket, binding.layoutBrandJacket, post.brandJacket)
        brandFormHelper.setBrandField(binding.etShoes, binding.layoutBrandShoes, post.brandShoes)
        brandFormHelper.setBrandField(binding.etBag, binding.layoutBrandBag, post.brandBag)
        brandFormHelper.setBrandField(binding.etDress, binding.layoutBrandDress, post.brandDress)
        brandFormHelper.setBrandField(binding.etGlasses, binding.layoutBrandGlasses, post.brandGlasses)
        brandFormHelper.setBrandField(binding.etAccessories, binding.layoutBrandAccessories, post.brandAccessories)

        val index = brandFormHelper.occasions.indexOf(post.occasion)
        if (index >= 0) {
            binding.spinnerOccasion.setSelection(index)
        } else if (post.occasion.isNotEmpty()) {
            binding.spinnerOccasion.setSelection(brandFormHelper.occasions.indexOf("other"))
            binding.layoutOccasionOther.visibility = View.VISIBLE
            binding.etOccasionOther.setText(post.occasion)
        }
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandFormHelper.occasions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                binding.layoutOccasionOther.visibility =
                    if (brandFormHelper.occasions[position] == "other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun saveChanges() {
        val selectedOccasion = brandFormHelper.occasions[binding.spinnerOccasion.selectedItemPosition]
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

        val description = binding.etDescription.text.toString().trim()
        val brandTop = brandFormHelper.getBrandValue(binding.etBrandTop)
        val brandBottom = brandFormHelper.getBrandValue(binding.etBrandBottom)
        val brandJacket = brandFormHelper.getBrandValue(binding.etJacket)
        val brandShoes = brandFormHelper.getBrandValue(binding.etShoes)
        val brandBag = brandFormHelper.getBrandValue(binding.etBag)
        val brandDress = brandFormHelper.getBrandValue(binding.etDress)
        val brandGlasses = brandFormHelper.getBrandValue(binding.etGlasses)
        val brandAccessories = brandFormHelper.getBrandValue(binding.etAccessories)

        pendingPost = post.copy(
            description = description,
            brandTop = brandTop,
            brandBottom = brandBottom,
            brandJacket = brandJacket,
            brandShoes = brandShoes,
            brandBag = brandBag,
            brandDress = brandDress,
            brandGlasses = brandGlasses,
            brandAccessories = brandAccessories,
            occasion = finalOccasion
        )

        viewModel.updatePost(
            postId = post.id,
            description = description,
            brandTop = brandTop,
            brandBottom = brandBottom,
            brandJacket = brandJacket,
            brandShoes = brandShoes,
            brandBag = brandBag,
            brandDress = brandDress,
            brandGlasses = brandGlasses,
            brandAccessories = brandAccessories,
            occasion = finalOccasion
        )
    }

    private fun setupObservers() {
        viewModel.editState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is EditState.Loading -> binding.btnSave.isEnabled = false
                is EditState.Success -> {
                    Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
                    pendingPost?.let { updated ->
                        parentFragmentManager.setFragmentResult(
                            "post_edited",
                            Bundle().apply { putSerializable("post", updated) }
                        )
                    }
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
