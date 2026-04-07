package com.example.stylish_android_application

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.example.stylish_android_application.databinding.FragmentAddPostBinding
import com.example.stylish_android_application.databinding.DialogBrandLinkBinding
import com.example.stylish_android_application.utils.ImageUtils
import com.example.stylish_android_application.viewmodel.AddPostViewModel
import com.example.stylish_android_application.viewmodel.UploadState
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    private lateinit var viewModel: AddPostViewModel

    // Maps each brand field to its stored URL (plain URL or composite "name||url")
    private val brandUrlMap = mutableMapOf<AutoCompleteTextView, String>()

    private val occasions = arrayOf("daily", "work", "brunch", "night out", "date", "wedding", "event", "other")

    private val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius", "reserved",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox", "Gali", "Fashion Club",
        "Carolina", "Blueberry", "Miss Nori", "Adika", "Yanga", "Cropp", "Aldo", "Black & white"
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

        viewModel = ViewModelProvider(this)[AddPostViewModel::class.java]

        setupSpinner()
        setupBrandAutocomplete()
        setupBrandLinkIcons()
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
            if (validateInput()) {
                startUploadProcess()
            }
        }
    }

    // --- Brand Link Icons ---

    private fun setupBrandLinkIcons() {
        val fields = listOf(
            binding.etBrandTop to binding.layoutBrandTop,
            binding.etBrandBottom to binding.layoutBrandBottom,
            binding.etJacket to binding.layoutBrandJacket,
            binding.etShoes to binding.layoutBrandShoes,
            binding.etBag to binding.layoutBrandBag,
            binding.etDress to binding.layoutBrandDress,
            binding.etGlasses to binding.layoutBrandGlasses,
            binding.etAccessories to binding.layoutBrandAccessories
        )
        for ((field, layout) in fields) {
            layout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            layout.setEndIconDrawable(R.drawable.ic_link)
            updateLinkIconTint(field, layout)
            layout.setEndIconOnClickListener { showLinkDialog(field, layout) }
        }
    }

    private fun updateLinkIconTint(field: AutoCompleteTextView, layout: TextInputLayout) {
        val hasLink = brandUrlMap.containsKey(field)
        val color = if (hasLink) Color.parseColor("#222222") else Color.parseColor("#BBBBBB")
        layout.setEndIconTintList(ColorStateList.valueOf(color))
    }

    private fun showLinkDialog(field: AutoCompleteTextView, layout: TextInputLayout) {
        val currentValue = brandUrlMap[field]
        val dialogBinding = DialogBrandLinkBinding.inflate(LayoutInflater.from(requireContext()))

        dialogBinding.tvDialogTitle.text = if (currentValue != null) "Edit Link" else "Add Link"
        dialogBinding.etDialogBrandName.setText(
            if (currentValue != null) BrandHelper.extractBrandName(currentValue)
            else field.text.toString().trim()
        )
        dialogBinding.etDialogUrl.setText(if (currentValue != null) BrandHelper.getUrl(currentValue) else "")
        if (currentValue != null) dialogBinding.btnRemoveLink.visibility = View.VISIBLE

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogBinding.btnSaveLink.setOnClickListener {
            val newName = dialogBinding.etDialogBrandName.text.toString().trim()
            val newUrl = dialogBinding.etDialogUrl.text.toString().trim()

            if (newUrl.isEmpty()) {
                brandUrlMap.remove(field)
                updateLinkIconTint(field, layout)
                dialog.dismiss()
                return@setOnClickListener
            }
            if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                Toast.makeText(requireContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            brandUrlMap[field] = BrandHelper.buildValue(newName, newUrl)
            if (field.text.isNullOrEmpty() && newName.isEmpty()) {
                field.setText(BrandHelper.extractBrandName(newUrl))
            } else if (newName.isNotEmpty()) {
                field.setText(newName)
            }
            updateLinkIconTint(field, layout)
            dialog.dismiss()
        }

        dialogBinding.btnRemoveLink.setOnClickListener {
            brandUrlMap.remove(field)
            updateLinkIconTint(field, layout)
            dialog.dismiss()
        }

        dialogBinding.btnCancelLink.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun getBrandValue(field: AutoCompleteTextView): String {
        val storedUrl = brandUrlMap[field]
        val typedName = field.text.toString().trim()
        return when {
            storedUrl != null && typedName.isNotEmpty() -> BrandHelper.buildValue(typedName, BrandHelper.getUrl(storedUrl))
            storedUrl != null -> storedUrl
            else -> typedName
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
        binding.etGlasses.setAdapter(adapter)
        binding.etAccessories.setAdapter(adapter)
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
                        brandTop = getBrandValue(binding.etBrandTop),
                        brandBottom = getBrandValue(binding.etBrandBottom),
                        occasion = finalOccasion,
                        brandJacket = getBrandValue(binding.etJacket),
                        brandShoes = getBrandValue(binding.etShoes),
                        brandBag = getBrandValue(binding.etBag),
                        brandDress = getBrandValue(binding.etDress),
                        brandGlasses = getBrandValue(binding.etGlasses),
                        brandAccessories = getBrandValue(binding.etAccessories)
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
