package com.aak.remotepresence.Authentication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aak.remotepresence.Authentication.Admin.AdminDashboardActivity
import com.aak.remotepresence.Authentication.User.UserDashboardActivity
import com.aak.remotepresence.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class WelcomeActivity : AppCompatActivity() {
    private lateinit var logo: ImageView
    private lateinit var topAnim: Animation
    private lateinit var bottomAnim: Animation

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        logo = findViewById(R.id.logo)

        topAnim = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation)

        logo.startAnimation(bottomAnim)
        topAnim.duration = 3000
        bottomAnim.duration = 3000

        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        checkUserSession()
    }

    private fun checkUserSession() {
        val currentUser = mAuth.currentUser

        // Delay to show welcome screen for 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            if (currentUser != null) {
                redirectToDashboard(currentUser.uid)
            } else {
                navigateToLogin()
            }
        }, 2000)
    }


    private fun redirectToDashboard(userId: String) {
        firestore.collection("admins").document(userId).get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    intent.putExtra("adminId", userId)
                    startActivity(intent)
                    finish()
                } else {
                    // Check User
                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            if (userDoc.exists()) {
                                val username = userDoc.getString("username") ?: ""
                                val userType = userDoc.getString("userType") ?: "User"

                                val intent = Intent(this, UserDashboardActivity::class.java)
                                intent.putExtra("userId", userId)
                                intent.putExtra("username", username)
                                intent.putExtra("userType", userType)
                                startActivity(intent)
                                finish()
                            } else {
                                navigateToLogin()
                            }
                        }
                        .addOnFailureListener {
                            navigateToLogin()
                        }
                }
            }
            .addOnFailureListener {
                navigateToLogin()
            }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
