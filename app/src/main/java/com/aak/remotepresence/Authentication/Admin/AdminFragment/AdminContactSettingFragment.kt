package com.aak.remotepresence.Authentication.Admin.AdminFragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aak.remotepresence.Authentication.Admin.AdminDashboardActivity
import com.aak.remotepresence.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminContactSettingFragment : Fragment() {

    private lateinit var whatsappNumberEditText: EditText
    private lateinit var callNumberEditText: EditText
    private lateinit var addButton: Button
    private lateinit var cancelButton: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val contactDocRef = firestore.collection("contact").document("admin_contact")

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_contact_setting, container, false)

        whatsappNumberEditText = view.findViewById(R.id.whatsappNumberEditText)
        callNumberEditText = view.findViewById(R.id.callNumberEditText)
        addButton = view.findViewById(R.id.addButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        loadExistingContact()

        addButton.setOnClickListener {
            val whatsapp = whatsappNumberEditText.text.toString().trim()
            val call = callNumberEditText.text.toString().trim()

            if (whatsapp.isEmpty() || call.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter both WhatsApp and Call numbers", Toast.LENGTH_SHORT).show()
            } else {
                saveContactNumbers(whatsapp, call)
            }
        }

        cancelButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }


        return view
    }

    private fun loadExistingContact() {
        contactDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val whatsapp = document.getString("whatsapp")
                    val call = document.getString("call")
                    whatsappNumberEditText.setText(whatsapp)
                    callNumberEditText.setText(call)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load contact", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveContactNumbers(whatsapp: String, call: String) {
        val data = hashMapOf(
            "whatsapp" to whatsapp,
            "call" to call
        )

        contactDocRef.set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Contact saved successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to save contact", Toast.LENGTH_SHORT).show()
            }
    }
}
