package com.aak.remotepresence.Authentication.Admin

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aak.remotepresence.R
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class AdminInprogressDetailActivity : AppCompatActivity() {
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_inprogress_detail)

        val orderId = intent.getStringExtra("orderId") ?: return

        val deleteButton: Button = findViewById(R.id.deleteTask)
        val completeButton: Button = findViewById(R.id.completeTask)
        val cancelButton: Button = findViewById(R.id.cancelTask)
        val backButton: ImageButton = findViewById(R.id.back_button)

        fetchOrderDetails(orderId)

        backButton.setOnClickListener { onBackPressed() }
        cancelButton.setOnClickListener { onBackPressed() }


        completeButton.setOnClickListener {
            markOrderAsComplete(orderId)
        }

        deleteButton.setOnClickListener {
            deleteOrder(orderId)
        }
    }

    private fun deleteOrder(orderId: String) {
        firestore.collection("tasks").document(orderId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Order deleted", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete order", Toast.LENGTH_SHORT).show()
            }
    }



    private fun fetchOrderDetails(orderId: String) {
        firestore.collection("tasks").document(orderId).get()
            .addOnSuccessListener { doc ->
                val userId = doc.getString("userId") ?: ""
                val category = doc.getString("category") ?: ""
                val detail = doc.getString("detail") ?: ""
                val instructions = doc.getString("instructions") ?: ""
                val location = doc.getString("location") ?: ""
                val status = doc.getString("status") ?: ""
                val urgency = doc.getString("urgency") ?: ""
                val mediaUri = doc.getString("mediaUri") ?: ""

                findViewById<TextView>(R.id.useridTextView).text = userId
                findViewById<TextView>(R.id.categoryTextView).text = category
                findViewById<TextView>(R.id.detailTextView).text = detail
                findViewById<TextView>(R.id.instructionsTextView).text = instructions
                findViewById<TextView>(R.id.locationTextView).text = location
                findViewById<TextView>(R.id.statusTxtView).text = status
                findViewById<TextView>(R.id.urgencyTxtView).text = urgency

                val imageView = findViewById<ImageView>(R.id.pictureImageView)

                if (mediaUri.isNotEmpty()) {
                    val extension = mediaUri.substringAfterLast('.', "").lowercase()

                    if (extension in listOf("jpg", "jpeg", "png", "gif")) {
                        // Load and show image
                        Picasso.get().load(mediaUri).into(imageView)

                        // On click, show download dialog
                        imageView.setOnClickListener {
                            showDownloadDialog(mediaUri, extension)
                        }
                    } else {
                        // If not image, hide the ImageView or set placeholder
                        imageView.setImageResource(R.drawable.image)
                        imageView.setOnClickListener(null)
                    }
                }

                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        findViewById<TextView>(R.id.userTextView).text = username
                    }
            }
    }

    private fun showDownloadDialog(url: String, extension: String) {
        val finalExtension = if (extension.isEmpty()) "jpg" else extension
        val fileName = "order_image_${System.currentTimeMillis()}.$finalExtension"

        AlertDialog.Builder(this)
            .setTitle("Download Image")
            .setMessage("Do you want to download this image?")
            .setPositiveButton("Download") { _, _ ->
                downloadFile(url, fileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun downloadFile(url: String, fileName: String) {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        val mimeType = when (extension) {
            "jpg", "jpeg", "png", "gif" -> "image/*"
            else -> "*/*"
        }

        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading image...")
                .setMimeType(mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadManager.enqueue(request)

            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun markOrderAsComplete(orderId: String) {
        firestore.collection("tasks").document(orderId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val userId = doc.getString("userId") ?: ""
                    val category = doc.getString("category") ?: ""
                    val detail = doc.getString("detail") ?: ""
                    val instructions = doc.getString("instructions") ?: ""
                    val urgency = doc.getString("urgency") ?: ""
                    val location = doc.getString("location") ?: ""
                    val mediaUri = doc.getString("mediaUri") ?: ""

                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Unknown"

                            val completeOrder = hashMapOf(
                                "userId" to userId,
                                "username" to username,
                                "category" to category,
                                "detail" to detail,
                                "instructions" to instructions,
                                "urgency" to urgency,
                                "location" to location,
                                "mediaUri" to mediaUri,
                                "status" to "Completed"
                            )

                            firestore.collection("completeOrders").document(orderId)
                                .set(completeOrder)
                                .addOnSuccessListener {
                                    firestore.collection("tasks").document(orderId).delete()
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Order marked as completed", Toast.LENGTH_SHORT).show()
                                            finish()
                                        }
                                }
                        }
                }
            }
    }
}
