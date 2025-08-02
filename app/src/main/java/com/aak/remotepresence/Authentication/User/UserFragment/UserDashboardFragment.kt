package com.aak.remotepresence.Authentication.User.UserFragment

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aak.remotepresence.Authentication.LoginActivity
import com.aak.remotepresence.Authentication.User.UserAdapter.UserRecentTaskAdapter
import com.aak.remotepresence.Authentication.User.UserModel.UserRecentTask
import com.aak.remotepresence.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserDashboardFragment : Fragment() {
    private var userId: String? = null
    private lateinit var newtask: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserRecentTaskAdapter
    private val taskList = mutableListOf<UserRecentTask>()
    private lateinit var viewAllTextView: TextView
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var giftLayout: LinearLayout
    private lateinit var shoppingLayout: LinearLayout
    private lateinit var medicineLayout: LinearLayout
    private lateinit var personalVisitLayout: LinearLayout
    private lateinit var docLayout: LinearLayout
    private lateinit var customreqLayout: LinearLayout
    private var alertDialog: AlertDialog? = null





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_dashboard, container, false)

        userId = arguments?.getString("userId")


        val logoutBtn = view.findViewById<ImageView>(R.id.sign_out_icon)
        newtask = view.findViewById(R.id.createTaskButton)
        viewAllTextView = view.findViewById(R.id.viewAllTasks)
        val whatsappBtn = view.findViewById<LinearLayout>(R.id.whatsappBtn)
        val callBtn = view.findViewById<LinearLayout>(R.id.callBtn)


        giftLayout = view.findViewById(R.id.gift)
        shoppingLayout = view.findViewById(R.id.shopping)
        medicineLayout = view.findViewById(R.id.medicine)
        personalVisitLayout = view.findViewById(R.id.personalvisit)
        docLayout = view.findViewById(R.id.doc)
        customreqLayout = view.findViewById(R.id.customreq)

        val layouts = listOf(giftLayout, shoppingLayout, medicineLayout, personalVisitLayout, docLayout, customreqLayout)

        for (layout in layouts) {
            layout.setOnClickListener {
                val fragment = UserCreateNewTaskFragment()
                val bundle = Bundle()
                bundle.putString("userId", userId)  // pass userId if needed
                fragment.arguments = bundle

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }


        val contactDocRef = firestore.collection("contact").document("admin_contact")

        whatsappBtn.setOnClickListener {
            contactDocRef.get().addOnSuccessListener { doc ->
                val number = doc.getString("whatsapp") ?: return@addOnSuccessListener
                showContactDialog("WhatsApp", number)
            }
        }

        callBtn.setOnClickListener {
            contactDocRef.get().addOnSuccessListener { doc ->
                val number = doc.getString("call") ?: return@addOnSuccessListener
                showContactDialog("Call", number)
            }
        }

        viewAllTextView.setOnClickListener {
            val allTasksFragment = UserAllTasksFragment()
            val bundle = Bundle()
            bundle.putString("userId", userId)
            allTasksFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, allTasksFragment)
                .addToBackStack(null)
                .commit()
        }



        newtask.setOnClickListener {
            val createTaskFragment = UserCreateNewTaskFragment()

            val bundle = Bundle()
            bundle.putString("userId", userId)
            createTaskFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, createTaskFragment) // Replace with actual container ID
                .addToBackStack(null)
                .commit()
        }

        recyclerView = view.findViewById(R.id.recentTasksRecycler)
        viewAllTextView = view.findViewById(R.id.viewAllTasks)

        adapter = UserRecentTaskAdapter(taskList) { task ->
            // Handle task item click – maybe open detail fragment
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        fetchRecentTasks()
        fetchAndShowNotification()






        logoutBtn.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes") { dialog, _ ->
                    // Perform logout
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(activity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    activity?.finish()
                    dialog.dismiss()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss() // Just dismiss the dialog
                }
                .setCancelable(false)
                .show()
        }
        if (userId != null) {
        } else {
            Log.e("UserDashboardFragment", "User ID is null")
        }

        return view
    }

    private fun fetchAndShowNotification() {
        userId?.let { uid ->
            val db = FirebaseFirestore.getInstance()
            val userClosedRef = db.collection("users").document(uid).collection("closedNotifications")

            db.collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { result ->
                    for (doc in result) {
                        val notificationId = doc.id
                        val title = doc.getString("title") ?: continue
                        val description = doc.getString("description") ?: continue

                        userClosedRef.document(notificationId).get().addOnSuccessListener { closedDoc ->
                            if (!closedDoc.exists()) {
                                showNotificationDialog(title, description, notificationId)
                            }
                        }
                    }
                }
        }
    }

    private fun showNotificationDialog(title: String, description: String, notificationId: String) {
        val context = requireContext()

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 40, 48, 24)
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val descView = TextView(context).apply {
            text = description
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 20)
        }

        val closeButton = Button(context).apply {
            text = "Close"
            setOnClickListener {
                // Save close status in Firestore
                userId?.let { uid ->
                    val closedRef = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .collection("closedNotifications")
                        .document(notificationId)

                    val closedData = mapOf("closedAt" to System.currentTimeMillis())
                    closedRef.set(closedData)
                }
                alertDialog?.dismiss()
            }
        }

        layout.addView(titleView)
        layout.addView(descView)
        layout.addView(closeButton)

        alertDialog = AlertDialog.Builder(context)
            .setView(layout)
            .setCancelable(false)
            .create()

        alertDialog?.show()
    }



    private fun showContactDialog(type: String, number: String) {
        val options = arrayOf("Copy", if (type == "WhatsApp") "Chat" else "Call")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select an action")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Copy
                        val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Contact", number)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(requireContext(), "$type number copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (type == "WhatsApp") {
                            val url = "https://wa.me/$number"
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(url)
                            startActivity(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = android.net.Uri.parse("tel:$number")
                            startActivity(intent)
                        }
                    }
                }
                dialog.dismiss()
            }
            .show()
    }


    private fun fetchRecentTasks() {
        userId?.let { uid ->
            firestore.collection("tasks")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "pending")
                .limit(3)
                .get()
                .addOnSuccessListener { documents ->
                    taskList.clear()
                    if (documents.isEmpty) {
                        Log.d("UserDashboard", "No tasks found for user $uid")
                    }
                    for (doc in documents) {
                        Log.d("UserDashboard", "Found task: ${doc.data}")
                        val task = doc.toObject(UserRecentTask::class.java).copy(taskId = doc.id)
                        taskList.add(task)
                    }
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("UserDashboard", "Failed to fetch tasks", e)
                }
        } ?: run {
            Log.e("UserDashboard", "User ID is null")
        }
    }



}