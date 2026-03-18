package com.example.stylish_android_application // תוודאי שזה תואם לשם החבילה שלך

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.stylish_android_application.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // טיפול בשוליים (שונה לשימוש ב-binding.main)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // בדיקת משתמש מחובר - נשאר בדיוק אותו דבר!
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // טעינת מסך הבית כברירת מחדל בהתחלה
        if (savedInstanceState == null) {
            replaceFragment(FeedFragment())
        }

        // --- השינוי שלנו: האזנה ללחיצות על האייקונים החדשים ---
        binding.navHome.setOnClickListener {
            replaceFragment(FeedFragment())
        }

        binding.navSearch.setOnClickListener {
            replaceFragment(SearchFragment())
        }

        binding.navAdd.setOnClickListener {
            replaceFragment(AddPostFragment())
        }

        binding.navLikes.setOnClickListener {
            replaceFragment(LikesFragment())
        }

        binding.navProfile.setOnClickListener {
            replaceFragment(ProfileFragment())
        }

        // --- ניהול כפתור החזור הפיזי של הטלפון ---
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 1. קודם כל בודקים אם יש מסך פנימי פתוח (כמו פרטי פוסט או מזג אוויר)
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack() // חוזרים מסך אחד אחורה באלגנטיות
                } else {
                    // 2. אם אין מסכים פנימיים, בודקים באיזה טאב אנחנו
                    val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
                    if (currentFragment is FeedFragment) {
                        finish() // יציאה מהאפליקציה
                    } else {
                        replaceFragment(FeedFragment()) // חזרה לפיד
                    }
                }
            }
        })
    }

    // פונקציה שמחליפה את ה-Fragment בתוך ה-Container
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}