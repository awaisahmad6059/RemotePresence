package com.aak.remotepresence.Authentication.User.UserFragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aak.remotepresence.Authentication.User.UserAdapter.UserMyTaskAdapter
import com.aak.remotepresence.Authentication.User.UserModel.UserRecentTask
import com.aak.remotepresence.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserMyTaskFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserMyTaskAdapter
    private val taskList = mutableListOf<UserRecentTask>()
    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null

    // Filter state
    private var currentStatusFilter = "pending" // Default filter

    // UI components for custom filters
    private lateinit var pendingText: TextView
    private lateinit var inProgressText: TextView
    private lateinit var completedText: TextView
    private lateinit var pendingUnderline: View
    private lateinit var inProgressUnderline: View
    private lateinit var completedUnderline: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_my_task, container, false)

        // RecyclerView Setup
        recyclerView = view.findViewById(R.id.allTasksRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserMyTaskAdapter(
            taskList,
            onItemClick = { task -> /* Handle task click */ },
            showContactDialog = { type, number -> showContactDialog(type, number) }
        )
        recyclerView.adapter = adapter

        // Initialize filter views
        pendingText = view.findViewById(R.id.pending_text)
        inProgressText = view.findViewById(R.id.in_progress_text)
        completedText = view.findViewById(R.id.completed_text)
        pendingUnderline = view.findViewById(R.id.pending_underline)
        inProgressUnderline = view.findViewById(R.id.in_progress_underline)
        completedUnderline = view.findViewById(R.id.completed_underline)

        // Set up click listeners
        view.findViewById<LinearLayout>(R.id.pending_button).setOnClickListener {
            currentStatusFilter = "pending"
            updateFilterUI()
            fetchAllTasks()
        }

        view.findViewById<LinearLayout>(R.id.in_progress_button).setOnClickListener {
            currentStatusFilter = "in progress"
            updateFilterUI()
            fetchAllTasks()
        }

        view.findViewById<LinearLayout>(R.id.completed_button).setOnClickListener {
            currentStatusFilter = "completed"
            updateFilterUI()
            fetchAllTasks()
        }

        // Initial state
        updateFilterUI()
        fetchAllTasks()

        return view
    }

    private fun updateFilterUI() {
        // Reset all to default state
        pendingText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        inProgressText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        completedText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))

        pendingText.setTypeface(null, Typeface.NORMAL)
        inProgressText.setTypeface(null, Typeface.NORMAL)
        completedText.setTypeface(null, Typeface.NORMAL)

        pendingUnderline.visibility = View.INVISIBLE
        inProgressUnderline.visibility = View.INVISIBLE
        completedUnderline.visibility = View.INVISIBLE

        // Apply selected state
        when (currentStatusFilter) {
            "pending" -> {
                pendingText.setTextColor(ContextCompat.getColor(requireContext(), R.color.btncolor))
                pendingText.setTypeface(null, Typeface.BOLD)
                pendingUnderline.visibility = View.VISIBLE
            }
            "in progress" -> {
                inProgressText.setTextColor(ContextCompat.getColor(requireContext(), R.color.btncolor))
                inProgressText.setTypeface(null, Typeface.BOLD)
                inProgressUnderline.visibility = View.VISIBLE
            }
            "completed" -> {
                completedText.setTextColor(ContextCompat.getColor(requireContext(), R.color.btncolor))
                completedText.setTypeface(null, Typeface.BOLD)
                completedUnderline.visibility = View.VISIBLE
            }
        }
    }

    private fun showContactDialog(type: String, number: String) {
        val options = arrayOf("Copy", if (type == "WhatsApp") "Chat" else "Call")

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select an action")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Copy
                        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Contact", number)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(requireContext(), "$type number copied", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        if (type == "WhatsApp") {
                            val url = "https://wa.me/$number"
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            startActivity(intent)
                        } else {
                            val intent = Intent(Intent.ACTION_DIAL)
                            intent.data = Uri.parse("tel:$number")
                            startActivity(intent)
                        }
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun fetchAllTasks() {
        userId?.let { uid ->
            val query: Query = if (currentStatusFilter == "completed") {
                firestore.collection("completeOrders")
                    .whereEqualTo("userId", uid)
            } else {
                firestore.collection("tasks")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("status", currentStatusFilter)
            }

            query.get()
                .addOnSuccessListener { documents ->
                    taskList.clear()
                    if (documents.isEmpty) {
                        Log.d("UserMyTaskFragment", "No tasks found for user $uid with status $currentStatusFilter")
                    }
                    for (doc in documents) {
                        val task = doc.toObject(UserRecentTask::class.java).copy(taskId = doc.id)
                        taskList.add(task)
                    }
                    // Sort by time, descending
                    taskList.sortByDescending { it.formattedTime } // This might fail if formattedTime is not a proper sortable string
                    adapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("UserMyTaskFragment", "Failed to fetch tasks", e)
                }
        } ?: run {
            Log.e("UserMyTaskFragment", "User ID is null")
        }
    }
}