package com.example.stylish_android_application

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.marginStart
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.stylish_android_application.databinding.FragmentLikesBinding
import com.example.stylish_android_application.viewmodel.LikesViewModel

class LikesFragment : Fragment() {

    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!

    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var postsAdapter: ProfileAdapter
    private lateinit var viewModel: LikesViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLikesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[LikesViewModel::class.java]

        setupAdapters()
        setupBackButton()
        setupObservers()
        setupBackButtonLogic() // הגדרת יירוט כפתור החזור הפיזי
    }

    private fun setupAdapters() {
        binding.rvFolders.layoutManager = GridLayoutManager(context, 2)
        foldersAdapter = FoldersAdapter(emptyList()) { selectedFolder ->
            openFolderUI(selectedFolder)
        }
        binding.rvFolders.adapter = foldersAdapter

        binding.rvLikedPosts.layoutManager = GridLayoutManager(context, 3)
        postsAdapter = ProfileAdapter(emptyList()) { post ->
            val fragment = PostDetailsFragment()
            val bundle = Bundle()
            bundle.putSerializable("post", post)
            fragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // מאפשר לחזור בדיוק לפה!
                .commit()
        }
        binding.rvLikedPosts.adapter = postsAdapter
    }

    // הקסם קורה פה: מאזינים לנתונים ומציירים את המסך בהתאם לזיכרון!
    private fun setupObservers() {
        viewModel.folders.observe(viewLifecycleOwner) { folders ->
            if (folders.isEmpty()) {
                binding.tvNoLikes.visibility = View.VISIBLE
                binding.rvFolders.visibility = View.GONE
                binding.rvLikedPosts.visibility = View.GONE
                binding.btnBack.visibility = View.GONE
            } else {
                binding.tvNoLikes.visibility = View.GONE
                foldersAdapter.updateFolders(folders)

                // בדיקה: האם הייתה תיקייה פתוחה כשיצאנו מהמסך?
                val openedName = viewModel.openedFolderName
                if (openedName != null) {
                    // מחפשים את התיקייה העדכנית לפי השם
                    val folderToOpen = folders.find { it.name == openedName }
                    if (folderToOpen != null) {
                        openFolderUI(folderToOpen) // פותחים אותה שוב!
                    } else {
                        // אם התיקייה נמחקה (כי הורדנו לייק לפוסט האחרון שבה) נסגור תצוגה
                        closeFolderUI()
                    }
                } else {
                    closeFolderUI() // מצב רגיל - מציגים את כל התיקיות
                }
            }
        }
    }

    private fun setupBackButton() {
        // כפתור החזור שעל המסך (למעלה שמאלה)
        binding.btnBack.setOnClickListener {
            closeFolderUI()
        }
    }

    private fun setupBackButtonLogic() {
        // ניהול כפתור החזור *הפיזי* של המכשיר
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewModel.openedFolderName != null) {
                    // אם אנחנו בתוך תיקייה - נסגור רק אותה
                    closeFolderUI()
                } else {
                    // אם אנחנו במסך התיקיות הראשי - ניתן למערכת לחזור לפיד הראשי
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    // --- שינויי תצוגה (UI) בלבד ---

    private fun openFolderUI(folder: FolderItem) {
        viewModel.openedFolderName = folder.name // שומרים בזיכרון של ה-ViewModel

        binding.rvFolders.visibility = View.GONE
        binding.rvLikedPosts.visibility = View.VISIBLE

        binding.btnBack.visibility = View.VISIBLE
        binding.tvLikesTitle.text = folder.name
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 4.dpToPx(requireContext())
        }

        postsAdapter.updatePosts(folder.posts)
    }

    private fun closeFolderUI() {
        viewModel.openedFolderName = null // מוחקים מהזיכרון

        binding.rvLikedPosts.visibility = View.GONE
        binding.rvFolders.visibility = View.VISIBLE

        binding.btnBack.visibility = View.GONE
        binding.tvLikesTitle.text = "Saved Looks"
        binding.tvLikesTitle.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            marginStart = 16.dpToPx(requireContext())
        }
    }

    private fun Int.dpToPx(context: android.content.Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}