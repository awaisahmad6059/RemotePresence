package com.aak.remotepresence.Authentication.User.UserFragment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.aak.remotepresence.R
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class UserCreateNewTaskFragment : Fragment() {

    private var userId: String? = null
    private var progressThread: Thread? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var selectedCategory: String = ""
    private var selectedUrgency = ""
    private lateinit var urgencyCards: List<CardView>

    private var mediaUri: Uri? = null
    private lateinit var addMediaLayout: LinearLayout
    private lateinit var backButton: ImageButton


    private lateinit var detailEt: EditText
    private lateinit var descEt: EditText
    private lateinit var locationEt: EditText
    private lateinit var submitBtn: Button

    private lateinit var categoryCards: List<CardView>
    private var progressDialog: Dialog? = null
    private var progressBar: ProgressBar? = null
    private var progressText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_create_new_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        detailEt = view.findViewById(R.id.detail)
        descEt = view.findViewById(R.id.editdesc)
        locationEt = view.findViewById(R.id.editlocation)
        submitBtn = view.findViewById(R.id.submitTaskButton)
        addMediaLayout = view.findViewById(R.id.addmedia)
        backButton = view.findViewById(R.id.back_button)

        backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        categoryCards = listOf(
            view.findViewById(R.id.cardGift),
            view.findViewById(R.id.cardShopping),
            view.findViewById(R.id.cardMedicine),
            view.findViewById(R.id.cardVisit),
            view.findViewById(R.id.cardDocuments),
            view.findViewById(R.id.cardCustom)
        )

        urgencyCards = listOf(
            view.findViewById(R.id.lowUrgency),
            view.findViewById(R.id.normalUrgency),
            view.findViewById(R.id.urgentUrgency)
        )

        setupCategoryCardClicks()
        setupUrgencyClicks()

        addMediaLayout.setOnClickListener {
            pickImageFromGallery()
        }

        submitBtn.setOnClickListener {
            submitTask()
        }
    }

    private fun setupCategoryCardClicks() {
        categoryCards.forEach { card ->
            card.setOnClickListener {
                categoryCards.forEach {
                    it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_default))
                }

                selectedCategory = when (card.id) {
                    R.id.cardGift -> "Gifts"
                    R.id.cardShopping -> "Shopping"
                    R.id.cardMedicine -> "Medicine"
                    R.id.cardVisit -> "Visit"
                    R.id.cardDocuments -> "Documents"
                    R.id.cardCustom -> "Custom"
                    else -> ""
                }

                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_selected))
            }
        }
    }

    private fun setupUrgencyClicks() {
        urgencyCards.forEach { card ->
            card.setOnClickListener {
                clearUrgencySelection()
                selectedUrgency = when (card.id) {
                    R.id.lowUrgency -> "Low"
                    R.id.normalUrgency -> "Normal"
                    R.id.urgentUrgency -> "Urgent"
                    else -> ""
                }
                card.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_selected))
            }
        }
    }

    private fun clearUrgencySelection() {
        urgencyCards.forEach {
            it.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.card_default))
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(Intent.createChooser(intent, "Select Image"), 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            mediaUri = data?.data
            mediaUri?.let {
                Toast.makeText(requireContext(), "Image selected", Toast.LENGTH_SHORT).show()
                showImagePreview(it)
            }
        }
    }

    private fun showImagePreview(uri: Uri) {
        addMediaLayout.removeAllViews()
        val imageView = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageURI(uri)
        }
        addMediaLayout.addView(imageView)
    }

    private fun submitTask() {
        val detail = detailEt.text.toString().trim()
        val desc = descEt.text.toString().trim()
        val location = locationEt.text.toString().trim()

        if (selectedCategory.isEmpty() || selectedUrgency.isEmpty() || detail.isEmpty() || desc.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields and select options.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = userId ?: auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not identified.", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val username = doc.getString("username") ?: ""
                val timestamp = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                val formattedDate = sdf.format(Date(timestamp))

                val taskData = hashMapOf<String, Any>(
                    "userId" to uid,
                    "username" to username,
                    "category" to selectedCategory,
                    "detail" to detail,
                    "instructions" to desc,
                    "location" to location,
                    "urgency" to selectedUrgency,
                    "formattedTime" to formattedDate,
                    "status" to "pending"
                )


                if (mediaUri != null) {
                    showProgressDialog()
                    thread {
                        try {
                            val url = uploadImageToCloudinary(mediaUri!!)
                            taskData["mediaUri"] = url ?: ""

                            requireActivity().runOnUiThread {
                                dismissProgressDialog()
                                saveTaskToFirestore(taskData)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            requireActivity().runOnUiThread {
                                dismissProgressDialog()
                                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    saveTaskToFirestore(taskData)
                }
            }
    }

    private fun uploadImageToCloudinary(uri: Uri): String? {
        val cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", "dewmsg60s",
                "api_key", "519592195627748",
                "api_secret", "v3_yR4o1MA9w4XOxCBfc9u7WfdE"
            )
        )

        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.asMap("resource_type", "image"))
        return uploadResult["secure_url"] as? String
    }

    private fun saveTaskToFirestore(taskData: HashMap<String, Any>) {
        firestore.collection("tasks")
            .add(taskData)
            .addOnSuccessListener {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Task submitted!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { e ->
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to submit: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showProgressDialog() {
        progressDialog = Dialog(requireContext()).apply {
            setCancelable(false)
            val layout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 40)
                setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            }

            progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                max = 100
                progress = 0
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 20
                }
            }

            progressText = TextView(context).apply {
                text = "Uploading... 0%"
                textSize = 16f
                gravity = Gravity.CENTER
            }

            layout.addView(progressBar)
            layout.addView(progressText)
            setContentView(layout)
            show()
        }

        // Start a background thread to simulate progress
        progressThread = Thread {
            var progress = 0
            try {
                while (progress < 100) {
                    Thread.sleep(50)
                    progress += 1
                    requireActivity().runOnUiThread {
                        progressBar?.progress = progress
                        progressText?.text = "Uploading... $progress%"
                    }
                }
            } catch (e: InterruptedException) {
                // Thread was interrupted; ignore
            }
        }
        progressThread?.start()
    }


    private fun dismissProgressDialog() {
        // Stop the thread if it's still running
        progressThread?.interrupt()
        progressThread = null

        requireActivity().runOnUiThread {
            if (progressDialog?.isShowing == true) {
                progressDialog?.dismiss()
                progressDialog = null
            }
        }
    }
}
