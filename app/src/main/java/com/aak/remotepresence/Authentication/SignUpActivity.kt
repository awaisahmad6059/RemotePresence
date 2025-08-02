package com.aak.remotepresence.Authentication

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aak.remotepresence.Authentication.User.UserDashboardActivity
import com.aak.remotepresence.R
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream

class SignUpActivity : AppCompatActivity() {

    private lateinit var alreadyHaveAccount: TextView
    private lateinit var inputEmail: EditText
    private lateinit var inputPassword: EditText
    private lateinit var inputConfirmPassword: EditText
    private lateinit var inputUsername: EditText
    private lateinit var btnRegister: Button
    private lateinit var back_button: ImageView
    private lateinit var progressDialog: ProgressDialog
    private lateinit var mAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var uploadPicBtn: Button
    private lateinit var imgProfile: de.hdodenhof.circleimageview.CircleImageView
    private var selectedImageUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        alreadyHaveAccount = findViewById(R.id.tv_sign_in)
        inputEmail = findViewById(R.id.et_email)
        inputPassword = findViewById(R.id.et_password)
        inputConfirmPassword = findViewById(R.id.et_confirm_password)
        inputUsername = findViewById(R.id.et_username)
        btnRegister = findViewById(R.id.btn_sign_up)
        back_button = findViewById(R.id.back_button)
        imgProfile = findViewById(R.id.imgProfile)
        uploadPicBtn = findViewById(R.id.btn_upload_picture)

        uploadPicBtn.setOnClickListener {
            pickImageFromGallery()
        }


        progressDialog = ProgressDialog(this)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        alreadyHaveAccount.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        back_button.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnRegister.setOnClickListener {
            performAuth()
        }
    }

    private val PICK_IMAGE_REQUEST = 1001

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            imgProfile.setImageURI(selectedImageUri)
        }
    }



    private fun performAuth() {
        val email = inputEmail.text.toString().trim()
        val password = inputPassword.text.toString().trim()
        val confirmPassword = inputConfirmPassword.text.toString().trim()
        val username = inputUsername.text.toString().trim()

        if (!email.matches(Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"))) {
            inputEmail.error = "Enter a correct email"
            return
        } else if (password.isEmpty() || password.length < 6) {
            inputPassword.error = "Enter a proper password"
            return
        } else if (password != confirmPassword) {
            inputConfirmPassword.error = "Passwords do not match"
            return
        }

        progressDialog.setMessage("Registering and uploading image...")
        progressDialog.setTitle("Registration")
        progressDialog.setCanceledOnTouchOutside(false)
        progressDialog.show()

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = mAuth.currentUser?.uid ?: return@addOnCompleteListener

                    if (selectedImageUri != null) {
                        uploadImageToCloudinary(selectedImageUri!!) { imageUrl ->
                            saveUserToFirestore(userId, email, username, password, imageUrl)
                        }
                    } else {
                        saveUserToFirestore(userId, email, username, password, null)
                    }

                } else {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String) -> Unit) {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 600, 600, true)

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()
        val encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        val cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", "dewmsg60s",
                "api_key", "519592195627748",
                "api_secret", "v3_yR4o1MA9w4XOxCBfc9u7WfdE"
            )
        )

        Thread {
            try {
                val uploadResult = cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap("resource_type", "image")
                )
                val imageUrl = uploadResult["secure_url"] as String
                runOnUiThread { callback(imageUrl) }
            } catch (e: Exception) {
                runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }


    private fun saveUserToFirestore(userId: String, email: String, username: String, password: String, imageUrl: String?) {
        val userMap = hashMapOf(
            "userId" to userId,
            "email" to email,
            "username" to username,
            "password" to password,
            "userType" to "User",
            "profileImageUrl" to (imageUrl ?: "") // Optional
        )

        firestore.collection("users")
            .document(userId)
            .set(userMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, UserDashboardActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("username", username)
                intent.putExtra("userType", "User")
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}