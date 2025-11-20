package com.aak.remotepresence.Authentication.User.UserFragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import com.aak.remotepresence.Authentication.LoginActivity
import com.aak.remotepresence.R
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

class UserProfileFragment : Fragment() {

    private lateinit var profileImage: CircleImageView
    private lateinit var btnChangePicture: Button
    private lateinit var userName: TextView
    private lateinit var completedTasksCount: TextView
    private lateinit var inProgressTasksCount: TextView
    private lateinit var pendingTasksCount: TextView
    private lateinit var helpSupportLayout: LinearLayout
    private lateinit var rateUsLayout: LinearLayout
    private lateinit var signout: LinearLayout
    private lateinit var progressBar: ProgressBar

    // Header container is a LinearLayout in XML, so use View (or LinearLayout)
    private lateinit var profileHeader: View
    private lateinit var profileDivider: View

    private val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    private val firestore = FirebaseFirestore.getInstance()
    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_profile, container, false)

        profileImage = view.findViewById(R.id.profileImage)
        btnChangePicture = view.findViewById(R.id.btn_change_picture)
        userName = view.findViewById(R.id.userName)
        completedTasksCount = view.findViewById(R.id.completedTasksCount)
        inProgressTasksCount = view.findViewById(R.id.inProgressTasksCount)
        pendingTasksCount = view.findViewById(R.id.pendingTasksCount)
        helpSupportLayout = view.findViewById(R.id.helpSupportLayout)
        rateUsLayout = view.findViewById(R.id.rateUsLayout) // Initialize rateUsLayout
        signout = view.findViewById(R.id.signoutlayout)
        progressBar = view.findViewById(R.id.profileUploadProgressBar)

        // Header + divider
        profileHeader = view.findViewById(R.id.profileHeader)   // <-- now View, not TextView
        profileDivider = view.findViewById(R.id.profileDivider)

        // Fade-in header container
        profileHeader.alpha = 0f
        profileHeader.animate()
            .alpha(1f)
            .setDuration(400)
            .setStartDelay(100)
            .start()

        // Slide + fade-in divider
        val dividerAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_slide_in)
        profileDivider.startAnimation(dividerAnim)

        loadUserData()
        loadTaskStats()

        btnChangePicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        helpSupportLayout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, UserAboutFragment())
                .addToBackStack(null)
                .commit()
        }

        // Add OnClickListener for Rate Us
        rateUsLayout.setOnClickListener {
            val packageName = "com.aak.remotepresence"
            try {
                // Try to open the Play Store app directly
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                // If Play Store is not installed, open in a web browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                startActivity(intent)
            }
        }

        signout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { dialog, _ ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
        }

        return view
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.data
            imageUri?.let {
                progressBar.visibility = View.VISIBLE
                uploadImageToCloudinary(it) { uploadedUrl ->
                    firestore.collection("users").document(uid)
                        .update("profileImageUrl", uploadedUrl)
                        .addOnSuccessListener {
                            Picasso.get().load(uploadedUrl).into(profileImage)
                            Toast.makeText(requireContext(), "Profile Updated", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Failed to update profile URL", Toast.LENGTH_SHORT).show()
                        }
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun uploadImageToCloudinary(imageUri: Uri, callback: (String) -> Unit) {
        val inputStream = requireContext().contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 600, 600, true)

        val baos = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val imageBytes = baos.toByteArray()

        val cloudinary = Cloudinary(
            ObjectUtils.asMap(
                "cloud_name", "dfbekjwhd",
                "api_key", "461365498914599",
                "api_secret", "6DA8ivrnu3gXAaaHhwQXZeAlk7w"
            )
        )

        Executors.newSingleThreadExecutor().execute {
            try {
                val uploadResult = cloudinary.uploader().upload(
                    imageBytes,
                    ObjectUtils.asMap("resource_type", "image")
                )
                val imageUrl = uploadResult["secure_url"] as String
                requireActivity().runOnUiThread { callback(imageUrl) }
            } catch (e: Exception) {
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadUserData() {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("username") ?: "No Name"
                val profileUrl = doc.getString("profileImageUrl")
                userName.text = name
                if (!profileUrl.isNullOrEmpty()) {
                    Picasso.get().load(profileUrl).into(profileImage)
                }
            }
    }

    private fun loadTaskStats() {
        firestore.collection("completeOrders")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener {
                completedTasksCount.text = it.size().toString()
            }

        firestore.collection("tasks")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { docs ->
                var pending = 0
                var inProgress = 0
                for (doc in docs) {
                    when (doc.getString("status")) {
                        "pending" -> pending++
                        "InProgress" -> inProgress++
                    }
                }
                pendingTasksCount.text = pending.toString()
                inProgressTasksCount.text = inProgress.toString()
            }
    }
}
