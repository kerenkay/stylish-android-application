package com.example.stylish_android_application.ui

import android.R
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.stylish_android_application.utils.BrandFormHelper
import com.example.stylish_android_application.ui.FeedFragment
import com.example.stylish_android_application.databinding.FragmentAddPostBinding
import com.example.stylish_android_application.utils.ImageUtils
import com.example.stylish_android_application.viewmodel.AddPostViewModel
import com.example.stylish_android_application.viewmodel.UploadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private lateinit var viewModel: AddPostViewModel
    private val brandFormHelper = BrandFormHelper(this, emptyFieldMeansBlank = false)

    private val cropImageLauncher = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val uriContent = result.uriContent
            selectedImageUri = uriContent

            binding.imgPostPreview.setImageURI(uriContent)
            binding.imgPostPreview.visibility = View.VISIBLE
            binding.imgPostPreview.scaleType = ImageView.ScaleType.CENTER_CROP

            binding.iconAdd.visibility = View.GONE
            binding.tvHint.visibility = View.GONE
        } else {
            result.error?.printStackTrace()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[AddPostViewModel::class.java]

        setupSpinner()
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
        setupObservers()

        binding.addPost.setOnClickListener {
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = null,
                    cropImageOptions = CropImageOptions(
                        imageSourceIncludeCamera = false,
                        imageSourceIncludeGallery = true,
                        aspectRatioX = 1,
                        aspectRatioY = 1,
                        fixAspectRatio = true,
                        showCropOverlay = true
                    )
                )
            )
        }

        binding.btnPost.setOnClickListener {
            if (validateInput()) startUploadProcess()
        }
    }

    private fun setupSpinner() {
        val adapter =
            ArrayAdapter(requireContext(), R.layout.simple_spinner_item, brandFormHelper.occasions)
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.layoutOccasionOther.visibility =
                    if (brandFormHelper.occasions[position] == "other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun validateInput(): Boolean {
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return false
        }
        val selectedOccasion = binding.spinnerOccasion.selectedItem.toString()
        if (selectedOccasion == "other" && binding.etOccasionOther.text.toString().trim().isEmpty()) {
            Toast.makeText(context, "Please specify the occasion", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setupObservers() {
        viewModel.uploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UploadState.Loading -> {
                    binding.btnPost.isEnabled = false
                    binding.btnPost.text = "Sharing..."
                }
                is UploadState.Success -> {
                    Toast.makeText(context, "Outfit shared successfully!", Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                    navigateToFeed()
                }
                is UploadState.Error -> {
                    Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                    binding.btnPost.isEnabled = true
                    binding.btnPost.text = "Share Outfit"
                    viewModel.resetState()
                }
                is UploadState.Idle -> { }
            }
        }
    }

    private fun startUploadProcess() {
        val uri = selectedImageUri ?: return
        var finalOccasion = binding.spinnerOccasion.selectedItem.toString()
        if (finalOccasion == "other") {
            finalOccasion = binding.etOccasionOther.text.toString().trim()
        }

        lifecycleScope.launch(Dispatchers.Default) {
            val imagePair = ImageUtils.processImageForUpload(requireContext(), uri)

            withContext(Dispatchers.Main) {
                if (imagePair != null) {
                    val (bitmap, imageBytes) = imagePair
                    viewModel.uploadPost(
                        bitmap = bitmap,
                        imageBytes = imageBytes,
                        description = binding.etDescription.text.toString().trim(),
                        brandTop = brandFormHelper.getBrandValue(binding.etBrandTop),
                        brandBottom = brandFormHelper.getBrandValue(binding.etBrandBottom),
                        occasion = finalOccasion,
                        brandJacket = brandFormHelper.getBrandValue(binding.etJacket),
                        brandShoes = brandFormHelper.getBrandValue(binding.etShoes),
                        brandBag = brandFormHelper.getBrandValue(binding.etBag),
                        brandDress = brandFormHelper.getBrandValue(binding.etDress),
                        brandGlasses = brandFormHelper.getBrandValue(binding.etGlasses),
                        brandAccessories = brandFormHelper.getBrandValue(binding.etAccessories)
                    )
                } else {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToFeed() {
        parentFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        parentFragmentManager.beginTransaction()
            .replace(
                com.example.stylish_android_application.R.id.fragment_container,
                FeedFragment()
            )
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}