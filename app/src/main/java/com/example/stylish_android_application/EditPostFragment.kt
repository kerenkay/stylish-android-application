package com.example.stylish_android_application

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.stylish_android_application.databinding.DialogBrandLinkBinding
import com.example.stylish_android_application.databinding.FragmentEditPostBinding
import com.example.stylish_android_application.viewmodel.EditPostViewModel
import com.example.stylish_android_application.viewmodel.EditState
import com.google.android.material.textfield.TextInputLayout

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: EditPostViewModel
    private lateinit var post: Post

    // Maps each brand field to its stored URL (plain URL or composite "name||url")
    private val brandUrlMap = mutableMapOf<AutoCompleteTextView, String>()

    // Holds the updated post to pass back to PostDetailsFragment on success
    private var pendingPost: Post? = null

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
        setupBrandLinkIcons()
        setupSpinner()
        populateFields()
        setupObservers()

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.btnSave.setOnClickListener { saveChanges() }
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

    // --- Field Population ---

    private fun populateFields() {
        binding.etDescription.setText(post.description)

        setBrandField(binding.etBrandTop, binding.layoutBrandTop, post.brandTop)
        setBrandField(binding.etBrandBottom, binding.layoutBrandBottom, post.brandBottom)
        setBrandField(binding.etJacket, binding.layoutBrandJacket, post.brandJacket)
        setBrandField(binding.etShoes, binding.layoutBrandShoes, post.brandShoes)
        setBrandField(binding.etBag, binding.layoutBrandBag, post.brandBag)
        setBrandField(binding.etDress, binding.layoutBrandDress, post.brandDress)
        setBrandField(binding.etGlasses, binding.layoutBrandGlasses, post.brandGlasses)
        setBrandField(binding.etAccessories, binding.layoutBrandAccessories, post.brandAccessories)

        val index = occasions.indexOf(post.occasion)
        if (index >= 0) {
            binding.spinnerOccasion.setSelection(index)
        } else if (post.occasion.isNotEmpty()) {
            binding.spinnerOccasion.setSelection(occasions.indexOf("other"))
            binding.layoutOccasionOther.visibility = View.VISIBLE
            binding.etOccasionOther.setText(post.occasion)
        }
    }

    private fun setBrandField(field: AutoCompleteTextView, layout: TextInputLayout, value: String) {
        if (BrandHelper.isUrl(value)) {
            field.setText(BrandHelper.extractBrandName(value))
            brandUrlMap[field] = value
        } else {
            field.setText(value)
        }
        updateLinkIconTint(field, layout)
    }

    // --- Save Logic ---

    private fun getBrandValue(field: AutoCompleteTextView): String {
        val storedUrl = brandUrlMap[field]
        val typedName = field.text.toString().trim()
        return when {
            typedName.isEmpty() -> ""
            storedUrl != null -> BrandHelper.buildValue(typedName, BrandHelper.getUrl(storedUrl))
            else -> typedName
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

        val description = binding.etDescription.text.toString().trim()
        val brandTop = getBrandValue(binding.etBrandTop)
        val brandBottom = getBrandValue(binding.etBrandBottom)
        val brandJacket = getBrandValue(binding.etJacket)
        val brandShoes = getBrandValue(binding.etShoes)
        val brandBag = getBrandValue(binding.etBag)
        val brandDress = getBrandValue(binding.etDress)
        val brandGlasses = getBrandValue(binding.etGlasses)
        val brandAccessories = getBrandValue(binding.etAccessories)

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

    // --- Boilerplate ---

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
