package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.stylish_android_application.databinding.FragmentAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    // רשימת האפשרויות
    private val occasions = arrayOf("daily", "work", "night out", "date", "wedding", "event", "other")

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

    private val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius", "reserved",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox", "Gali"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupBrandAutocomplete()

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
                uploadPostImage()
            }
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
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, occasions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (occasions[position] == "other") {
                    binding.layoutOccasionOther.visibility = View.VISIBLE
                } else {
                    binding.layoutOccasionOther.visibility = View.GONE
                }
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

    private fun uploadPostImage() {
        binding.btnPost.isEnabled = false
        binding.btnPost.text = "Sharing..."

        try {
            var bitmap = uriToBitmap(selectedImageUri!!)

            // --- הקסם למהירות: כיווץ התמונה לפני ההעלאה! ---
            // 1. מקטינים רזולוציה ל-1080 (סטנדרט של רשתות חברתיות)
            val maxResolution = 1080
            if (bitmap.width > maxResolution || bitmap.height > maxResolution) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val newWidth = if (ratio > 1) maxResolution else (maxResolution * ratio).toInt()
                val newHeight = if (ratio > 1) (maxResolution / ratio).toInt() else maxResolution
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            }

            val baos = ByteArrayOutputStream()
            // 2. דוחסים את משקל הקובץ (איכות של 75% במקום 100% חוסכת המון מקום ולא מורגשת)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            val data = baos.toByteArray()
            // --------------------------------------------------

            val fileName = UUID.randomUUID().toString() + ".jpg"
            val storageRef = FirebaseStorage.getInstance().reference.child("post_images/$fileName")

            storageRef.putBytes(data)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        analyzeOutfitWithAI(bitmap, imageUrl)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                    resetButton()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
            resetButton()
        }
    }

    private fun analyzeOutfitWithAI(bitmap: Bitmap, imageUrl: String) {
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        val prompt = """
            Look at the clothing in this image. Classify the outfit into exactly one of these three weather categories: 
            - 'Hot' (for summer/hot weather, shorts, t-shirts, light dresses)
            - 'Warm' (for spring/autumn/mild weather, long sleeves, light jackets)
            - 'Cold' (for winter/freezing weather, heavy coats, sweaters, scarves)
            Reply with ONLY ONE WORD from the categories above (Hot, Warm, or Cold).
        """.trimIndent()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })

                val rawAnswer = response.text?.lowercase() ?: ""
                var aiCategory = "Warm"
                if (rawAnswer.contains("hot")) {
                    aiCategory = "Hot"
                } else if (rawAnswer.contains("cold")) {
                    aiCategory = "Cold"
                }

                withContext(Dispatchers.Main) {
                    saveToFirestore(imageUrl, aiCategory)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    saveToFirestore(imageUrl, "Warm")
                }
            }
        }
    }

    private fun saveToFirestore(imageUrl: String, aiCategory: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "You are not logged in!", Toast.LENGTH_SHORT).show()
            resetButton()
            return
        }

        val name = currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "Guest"

        var finalOccasion = binding.spinnerOccasion.selectedItem.toString()
        if (finalOccasion == "other") {
            finalOccasion = binding.etOccasionOther.text.toString().trim()
        }

        val post = Post(
            userId = currentUser.uid,
            userName = name,
            imageUrl = imageUrl,
            description = binding.etDescription.text.toString().trim(),
            brandTop = binding.etBrandTop.text.toString().trim(),
            brandBottom = binding.etBrandBottom.text.toString().trim(),
            occasion = finalOccasion,
            brandJacket = binding.etJacket.text.toString().trim(),
            brandShoes = binding.etShoes.text.toString().trim(),
            brandBag = binding.etBag.text.toString().trim(),
            brandDress = binding.etDress.text.toString().trim(),
            weatherCategory = aiCategory,
            timestamp = System.currentTimeMillis()
        )

        FirebaseFirestore.getInstance().collection("posts")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(context, "Post shared! AI classified as: $aiCategory", Toast.LENGTH_LONG).show()
                resetButton()

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
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                resetButton()
            }
    }

    // הפונקציה התקצרה משמעותית כי הקרופר עושה את העבודה הקשה בשבילנו!
    private fun uriToBitmap(uri: Uri): Bitmap {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        return bitmap!! // כאן אנחנו בטוחים שיש תמונה כי כבר עברנו את החיתוך
    }

    private fun resetButton() {
        binding.btnPost.isEnabled = true
        binding.btnPost.text = "Share Outfit"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}