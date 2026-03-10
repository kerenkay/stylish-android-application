package com.example.stylish_android_application

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.stylish_android_application.databinding.FragmentAddPostBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import android.media.ExifInterface
import android.graphics.Matrix
import androidx.lifecycle.lifecycleScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    private var selectedImageUri: Uri? = null

    // רשימת האפשרויות
    private val occasions = arrayOf("daily", "work", "night out", "date", "wedding", "event", "other")

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.imgPostPreview.setImageURI(uri)
//            binding.imgPostPreview.visibility = View.VISIBLE
//            binding.imgPostPreview.scaleType = ImageView.ScaleType.CENTER_CROP
//            binding.imgPostPreview.imageTintList = null

            binding.imgPostPreview.visibility = View.VISIBLE
            binding.iconAdd.visibility = View.GONE
            binding.tvHint.visibility = View.GONE
            binding.imgPostPreview.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    private val brandSuggestions = arrayOf(
        "Zara", "H&M", "Nike", "Adidas", "Pull & Bear", "Bershka", "Stradivarius",
        "Mango", "Massimo Dutti", "Levi's", "Diesel", "Castro", "Renuar", "Twentyfourseven",
        "Urban Outfitters", "ASOS", "Shein", "Gucci", "Prada", "Chanel", "Jordan",
        "New Balance", "Vans", "Converse", "Puma", "Terminal X", "Fox"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner() // הפעלת ה-Spinner
        setupBrandAutocomplete()

        binding.addPost.setOnClickListener { pickImageLauncher.launch("image/*") }

        binding.btnPost.setOnClickListener {
            if (validateInput()) {
                uploadPostAsBase64()
            }
        }
    }

    // --- הגדרת רשימת הבחירה (Spinner) ---

    private fun setupBrandAutocomplete() {
        // יוצרים אדפטר שייקח את רשימת המותגים ויעצב אותם כשורות נפתחות
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, brandSuggestions)

        // מחברים את האדפטר לכל אחת מתיבות הטקסט הרלוונטיות
        binding.etBrandTop.setAdapter(adapter)
        binding.etBrandBottom.setAdapter(adapter)
        binding.etJacket.setAdapter(adapter)
        binding.etShoes.setAdapter(adapter)
        binding.etBag.setAdapter(adapter)
        binding.etDress.setAdapter(adapter)
    }
    private fun setupSpinner() {
        // יצירת מתאם בין הרשימה לרכיב הגרפי
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, occasions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOccasion.adapter = adapter

        // האזנה לשינויים בבחירה
        binding.spinnerOccasion.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = occasions[position]
                // אם נבחר "other", מציגים את שדה הטקסט החופשי. אחרת - מסתירים.
                if (selected == "other") {
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

        // בדיקה: אם נבחר "other", חובה למלא את הטקסט החופשי
        val selectedOccasion = binding.spinnerOccasion.selectedItem.toString()
        if (selectedOccasion == "other" && binding.etOccasionOther.text.toString().trim().isEmpty()) {
            Toast.makeText(context, "Please specify the occasion", Toast.LENGTH_SHORT).show()
            return false
        }

        // הערה: הסרנו את הבדיקה על ה-Description כי ביקשת שזה לא יהיה חובה
        return true
    }

    private fun uploadPostAsBase64() {
        binding.btnPost.isEnabled = false
        binding.btnPost.text = "Sharing..."

        try {
            val bitmap = uriToBitmap(selectedImageUri!!)
            val imageBase64 = bitmapToBase64(bitmap)
            analyzeOutfitWithAI(bitmap, imageBase64)
//            saveToFirestore(imageBase64)
        } catch (e: Exception) {
            Log.e("Upload", "Error converting image", e)
            Toast.makeText(context, "Error processing image", Toast.LENGTH_SHORT).show()
            resetButton()
        }
    }

    private fun analyzeOutfitWithAI(bitmap: Bitmap, imageBase64: String) {
        // אתחול המודל של Gemini (נשים את מפתח ה-API פה זמנית)
        //val smallBitmap = Bitmap.createScaledBitmap(bitmap, 500, 500, true)
        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            // שימי פה את מפתח ה-API שניצור בשלב הבא!
            apiKey = BuildConfig.GEMINI_API_KEY
        )

        val prompt = """
            Look at the clothing in this image. Classify the outfit into exactly one of these three weather categories: 
            - 'Hot' (for summer/hot weather, shorts, t-shirts, light dresses)
            - 'Warm' (for spring/autumn/mild weather, long sleeves, light jackets)
            - 'Cold' (for winter/freezing weather, heavy coats, sweaters, scarves)
            Reply with ONLY ONE WORD from the categories above (Hot, Warm, or Cold).
        """.trimIndent()

        // מריצים את בקשת ה-AI ברקע (כדי שהאפליקציה לא תקרוס)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(content {
                    image(bitmap)
                    text(prompt)
                })

                // לוקחים את התשובה והופכים אותה לאותיות קטנות כדי למנוע בעיות
                val rawAnswer = response.text?.lowercase() ?: ""
                Log.d("Gemini_Test", "AI Answered: $rawAnswer") // ידפיס לנו ב-Logcat מה ה-AI באמת אמר!

                // חיפוש חכם: בודקים אם המילה מופיעה בתוך התשובה, ולא דורשים התאמה מושלמת
                var aiCategory = "Warm" // ברירת המחדל
                if (rawAnswer.contains("hot")) {
                    aiCategory = "Hot"
                } else if (rawAnswer.contains("cold")) {
                    aiCategory = "Cold"
                }

                // חוזרים למסך הראשי (Main Thread) כדי לשמור את הפוסט
                withContext(Dispatchers.Main) {
                    saveToFirestore(imageBase64, aiCategory)
                }

            } catch (e: Exception) {
                // אם יש שגיאה (כמו חוסר באינטרנט או בעיה במפתח) נראה אותה כאן!
                Log.e("Gemini_Error", "API Failed: ${e.message}")

                withContext(Dispatchers.Main) {
                    saveToFirestore(imageBase64, "Warm") // במקרה של שגיאה אמיתית, נשמור כ-Warm
                }
            }
        }
    }

    private fun saveToFirestore(imageBase64: String, aiCategory: String) {
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
            userName = name, // <--- הנה השם החדש והיפה!
            imageUrl = imageBase64,
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
                Toast.makeText(context, "Post shared successfully!", Toast.LENGTH_LONG).show()
                Toast.makeText(context, "Post shared! AI classified as: $aiCategory", Toast.LENGTH_LONG).show()
                // 1. קודם כל משחררים את הכפתור ומנקים את הטופס (כדי שלא ייתקע)
                resetButton()

                // 2. ניווט חזרה לדף הבית (Feed)
                // ננסה לחזור אחורה, ואם זה לא עובד - נחליף טאב ידנית
                if (parentFragmentManager.backStackEntryCount > 0) {
                    parentFragmentManager.popBackStack()
                } else {
                    // אם אנחנו בתוך טאבים, צריך לגשת ל-Activity ולהעביר לטאב הבית
                    // (נניח שה-ID של ה-BottomNav הוא bottomNavigationView ושל הבית הוא homeFragment)
                    // אם השמות אצלך שונים, תצטרכי להתאים את השורה הזו או למחוק אותה
                    try {
//                        val bottomNav = requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
//                        bottomNav?.selectedItemId = R.id.nav_home // ודאי שזה ה-ID של טאב הבית אצלך
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, FeedFragment())
                            .commit()
                    } catch (e: Exception) {
                        // אם לא הצלחנו לנווט, לפחות הטופס נקי והכפתור משוחרר
                    }
                }
            }
            .addOnFailureListener { e ->
                if (e.message?.contains("exceeds the maximum") == true) {
                    Toast.makeText(context, "Image is too big", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                resetButton()
            }
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        // 1. קריאת התמונה מהזיכרון
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        var bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // 2. הקטנת התמונה (כדי שלא נקבל OutOfMemory)
        val maxDimension = 600
        if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val ratio = bitmap.width.toDouble() / bitmap.height.toDouble()
            val newWidth = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // 3. התיקון: קריאת ה-Exif וסיבוב התמונה במידת הצורך
        var rotatedBitmap = bitmap
        try {
            // פותחים זרם חדש כדי לקרוא את המידע הנסתר
            val exifInputStream = requireContext().contentResolver.openInputStream(uri)
            if (exifInputStream != null) {
                val exif = ExifInterface(exifInputStream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                // אם זיהינו שצריך לסובב, אנחנו מסובבים את ה-Bitmap
                if (orientation != ExifInterface.ORIENTATION_NORMAL && orientation != ExifInterface.ORIENTATION_UNDEFINED) {
                    rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                exifInputStream.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return rotatedBitmap
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
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