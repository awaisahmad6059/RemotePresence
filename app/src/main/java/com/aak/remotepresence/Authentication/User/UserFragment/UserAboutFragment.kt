package com.aak.remotepresence.Authentication.User.UserFragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aak.remotepresence.R
import com.google.firebase.firestore.FirebaseFirestore

class UserAboutFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private val contactDocRef = firestore.collection("contact").document("admin_contact")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_about, container, false)

        val whatsappBtn = view.findViewById<View>(R.id.whatsappBtn)
        val callBtn = view.findViewById<View>(R.id.callBtn)
        val tvWhatsapp = view.findViewById<TextView>(R.id.tvWhatsappNumber)
        val tvCall = view.findViewById<TextView>(R.id.tvPhoneNumber)

        // Get and set contact numbers
        contactDocRef.get().addOnSuccessListener { document ->
            document.getString("whatsapp")?.let { whatsapp ->
                tvWhatsapp.text = whatsapp
            }
            document.getString("call")?.let { call ->
                tvCall.text = call
            }
        }

        whatsappBtn.setOnClickListener {
            contactDocRef.get().addOnSuccessListener { doc ->
                doc.getString("whatsapp")?.let { number ->
                    showContactDialog("WhatsApp", number)
                }
            }
        }

        callBtn.setOnClickListener {
            contactDocRef.get().addOnSuccessListener { doc ->
                doc.getString("call")?.let { number ->
                    showContactDialog("Call", number)
                }
            }
        }

        return view
    }

    private fun showContactDialog(type: String, number: String) {
        val options = arrayOf("Copy", if (type == "WhatsApp") "Chat" else "Call")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("$type Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Contact", number)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "$type number copied", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (type == "WhatsApp") {
                            val url = "https://wa.me/$number"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                            startActivity(intent)
                        }
                    }
                }
                dialog.dismiss()
            }
            .show()
    }
}
