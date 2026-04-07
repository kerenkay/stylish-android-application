package com.example.stylish_android_application

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.stylish_android_application.databinding.DialogBrandLinkBinding
import com.google.android.material.textfield.TextInputLayout

/**
 * Delegate that holds all shared brand-link UI logic for post forms.
 *
 * @param fragment      the host fragment (used for context/resources inside method bodies)
 * @param emptyFieldMeansBlank  true for EditPost — clearing a field returns "" (removes brand);
 *                              false for AddPost — an empty field with a stored URL returns the URL
 */
class BrandFormHelper(
    private val fragment: Fragment,
    private val emptyFieldMeansBlank: Boolean = false
) {

    val occasions = arrayOf("daily", "work", "brunch", "night out", "date", "wedding", "event", "other")

    val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius", "reserved",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox", "Gali", "Fashion Club",
        "Carolina", "Blueberry", "Miss Nori", "Adika", "Yanga", "Cropp", "Aldo", "Black & white"
    )

    private val brandUrlMap = mutableMapOf<AutoCompleteTextView, String>()

    fun setupBrandAutocomplete(vararg fields: AutoCompleteTextView) {
        val adapter = ArrayAdapter(
            fragment.requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            brandSuggestions
        )
        fields.forEach { it.setAdapter(adapter) }
    }

    fun setupBrandLinkIcons(fields: List<Pair<AutoCompleteTextView, TextInputLayout>>) {
        for ((field, layout) in fields) {
            layout.endIconMode = TextInputLayout.END_ICON_CUSTOM
            layout.setEndIconDrawable(R.drawable.ic_link)
            updateLinkIconTint(field, layout)
            layout.setEndIconOnClickListener { showLinkDialog(field, layout) }
        }
    }

    fun updateLinkIconTint(field: AutoCompleteTextView, layout: TextInputLayout) {
        val color = if (brandUrlMap.containsKey(field)) Color.parseColor("#222222") else Color.parseColor("#BBBBBB")
        layout.setEndIconTintList(ColorStateList.valueOf(color))
    }

    fun showLinkDialog(field: AutoCompleteTextView, layout: TextInputLayout) {
        val currentValue = brandUrlMap[field]
        val ctx = fragment.requireContext()
        val dialogBinding = DialogBrandLinkBinding.inflate(LayoutInflater.from(ctx))

        dialogBinding.tvDialogTitle.text = if (currentValue != null) "Edit Link" else "Add Link"
        dialogBinding.etDialogBrandName.setText(
            if (currentValue != null) BrandHelper.extractBrandName(currentValue)
            else field.text.toString().trim()
        )
        dialogBinding.etDialogUrl.setText(if (currentValue != null) BrandHelper.getUrl(currentValue) else "")
        if (currentValue != null) dialogBinding.btnRemoveLink.visibility = View.VISIBLE

        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                (fragment.resources.displayMetrics.widthPixels * 0.9).toInt(),
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
                Toast.makeText(ctx, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
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

    fun getBrandValue(field: AutoCompleteTextView): String {
        val storedUrl = brandUrlMap[field]
        val typedName = field.text.toString().trim()
        return when {
            emptyFieldMeansBlank && typedName.isEmpty() -> ""
            storedUrl != null && typedName.isNotEmpty() -> BrandHelper.buildValue(typedName, BrandHelper.getUrl(storedUrl))
            storedUrl != null -> storedUrl
            else -> typedName
        }
    }

    fun setBrandField(field: AutoCompleteTextView, layout: TextInputLayout, value: String) {
        if (BrandHelper.isUrl(value)) {
            field.setText(BrandHelper.extractBrandName(value))
            brandUrlMap[field] = value
        } else {
            field.setText(value)
        }
        updateLinkIconTint(field, layout)
    }
}
