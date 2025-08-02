package com.aak.remotepresence.Authentication

import android.app.ProgressDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aak.remotepresence.Authentication.Admin.AdminDashboardActivity
import com.aak.remotepresence.Authentication.User.UserDashboardActivity
import com.aak.remotepresence.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvSignUp: TextView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        inputEmail = findViewById(R.id.et_email)
        inputPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_sign_in)
        tvSignUp = findViewById(R.id.tv_sign_up)
        progressDialog = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnLogin.setOnClickListener {
            loginUser()
        }

    }









    private fun loginUser() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
            return
        }

        progressDialog.setMessage("Logging in...")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val userId = mAuth.currentUser?.uid ?: return@addOnSuccessListener

                firestore.collection("admins").document(userId).get()
                    .addOnSuccessListener { adminDoc ->
                        if (adminDoc.exists()) {
                            progressDialog.dismiss()
                            val intent = Intent(this, AdminDashboardActivity::class.java)
                            intent.putExtra("adminId", userId)
                            startActivity(intent)
                            finish()
                        } else {
                            // If not found in admins, check in users
                            firestore.collection("users").document(userId).get()
                                .addOnSuccessListener { userDoc ->
                                    progressDialog.dismiss()
                                    if (userDoc.exists()) {
                                        val username = userDoc.getString("username") ?: "Unknown"

                                        val intent = Intent(this, UserDashboardActivity::class.java)
                                        intent.putExtra("userId", userId)
                                        intent.putExtra(
                                            "username",
                                            username
                                        ) // ✅ passing username here
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "User not found in any collection",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .addOnFailureListener {
                                    progressDialog.dismiss()
                                    Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT)
                                        .show()
                                }
                        }

                        }
                    .addOnFailureListener {
                        progressDialog.dismiss()
                        Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Login failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
