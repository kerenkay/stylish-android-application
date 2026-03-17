package com.example.stylish_android_application

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

    // 1. Declare the ViewModel
    private lateinit var viewModel: AddPostViewModel

    private val occasions = arrayOf("daily", "work", "night out", "date", "wedding", "event", "other")

    private val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius", "reserved",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox", "Gali"
    )

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

        // 2. Initialize the ViewModel
        viewModel = ViewModelProvider(this)[AddPostViewModel::class.java]

        setupSpinner()
        setupBrandAutocomplete()
        setupObservers() // Listen to the ViewModel's state

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
            if (validateInput()) {
                startUploadProcess()
            }
        }
    }

    // --- UI Setup Methods ---

    private fun setupBrandAutocomplete() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brandSuggestions)
        binding.etBrandTop.setAdapter(adapter)
        binding.etBrandBottom.setAdapter(adapter)
        binding.etJacket.setAdapter(adapter)
        binding.etShoes.setAdapter(adapter)
        binding.etBag.setAdapter(adapter)
        binding.etDress.setAdapter(adapter)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, occasions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.layoutOccasionOther.visibility = if (occasions[position] == "other") View.VISIBLE else View.GONE
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

    // --- Architecture Magic: Observing and Delegating ---

    private fun setupObservers() {
        // 3. The Fragment only listens and updates the UI based on the state
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
                is UploadState.Idle -> {
                    // Do nothing, just waiting for user action
                }
            }
        }
    }

    private fun startUploadProcess() {
        val uri = selectedImageUri ?: return
        var finalOccasion = binding.spinnerOccasion.selectedItem.toString()
        if (finalOccasion == "other") {
            finalOccasion = binding.etOccasionOther.text.toString().trim()
        }

        // Process the image on a background thread to avoid freezing the UI
        lifecycleScope.launch(Dispatchers.Default) {
            val imagePair = ImageUtils.processImageForUpload(requireContext(), uri)

            withContext(Dispatchers.Main) {
                if (imagePair != null) {
                    val (bitmap, imageBytes) = imagePair

                    // 4. Pass everything to the ViewModel to handle the business logic!
                    viewModel.uploadPost(
                        bitmap = bitmap,
                        imageBytes = imageBytes,
                        description = binding.etDescription.text.toString().trim(),
                        brandTop = binding.etBrandTop.text.toString().trim(),
                        brandBottom = binding.etBrandBottom.text.toString().trim(),
                        occasion = finalOccasion,
                        brandJacket = binding.etJacket.text.toString().trim(),
                        brandShoes = binding.etShoes.text.toString().trim(),
                        brandBag = binding.etBag.text.toString().trim(),
                        brandDress = binding.etDress.text.toString().trim()
                    )
                } else {
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun navigateToFeed() {
        if (parentFragmentManager.backStackEntryCount > 0) {
            parentFragmentManager.popBackStack()
        } else {
            try {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, FeedFragment())
                    .commit()
            } catch (e: Exception) { }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}