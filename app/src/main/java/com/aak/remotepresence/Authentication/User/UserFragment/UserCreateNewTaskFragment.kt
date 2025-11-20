package com.aak.remotepresence.Authentication.User.UserFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aak.remotepresence.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserCreateNewTaskFragment : Fragment() {

    private var userId: String? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var selectedCategory: String = ""
    private var selectedUrgency = ""
    private lateinit var urgencyCards: List<CardView>

    private lateinit var detailEt: EditText
    private lateinit var contact: EditText
    private lateinit var descEt: EditText
    private lateinit var locationEt: EditText
    private lateinit var submitBtn: Button
    private lateinit var backButton: ImageButton

    private lateinit var categoryCards: List<CardView>

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
        contact = view.findViewById(R.id.contact)
        locationEt = view.findViewById(R.id.editlocation)
        submitBtn = view.findViewById(R.id.submitTaskButton)
        backButton = view.findViewById(R.id.back_button)

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

        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserDashboardFragment())
                .commit()
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

    private fun submitTask() {
        val detail = detailEt.text.toString().trim()
        val contactNum = contact.text.toString().trim()
        val desc = descEt.text.toString().trim()
        val location = locationEt.text.toString().trim()

        if (selectedCategory.isEmpty() || detail.isEmpty() || contactNum.isEmpty() || desc.isEmpty() || location.isEmpty() || selectedUrgency.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields and select options.", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = userId ?: auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "User not identified.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(requireContext(), "Submitting task...", Toast.LENGTH_SHORT).show()

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
                    "contact" to contactNum,
                    "instructions" to desc,
                    "location" to location,
                    "urgency" to selectedUrgency,
                    "formattedTime" to formattedDate,
                    "status" to "pending"
                )
                saveTaskToFirestore(taskData)
            }
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
}
