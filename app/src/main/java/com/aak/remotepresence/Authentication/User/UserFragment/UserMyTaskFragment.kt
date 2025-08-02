package com.aak.remotepresence.Authentication.User.UserFragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aak.remotepresence.Authentication.User.UserAdapter.UserMyTaskAdapter
import com.aak.remotepresence.Authentication.User.UserModel.UserRecentTask
import com.aak.remotepresence.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class UserMyTaskFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserMyTaskAdapter
    private val taskList = mutableListOf<UserRecentTask>()
    private val firestore = FirebaseFirestore.getInstance()
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getString("userId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_my_task, container, false)
        recyclerView = view.findViewById(R.id.allTasksRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = UserMyTaskAdapter(
            taskList,
            onItemClick = { task -> /* Handle task click */ },
            showContactDialog = { type, number -> showContactDialog(type, number) }
        )

        recyclerView.adapter = adapter
        fetchAllTasks()
        return view
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


    private fun openWhatsApp(number: String) {
        try {
            val url = "https://wa.me/$number"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("WhatsApp", "Error opening WhatsApp", e)
        }
    }

    private fun makePhoneCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("PhoneCall", "Error making call", e)
        }
    }

    private fun fetchAllTasks() {
        userId?.let { uid ->
            val combinedTaskList = mutableListOf<UserRecentTask>()
            val pendingTasksQuery = firestore.collection("tasks").whereEqualTo("userId", uid)
            val completedTasksQuery = firestore.collection("completeOrders").whereEqualTo("userId", uid)

            Tasks.whenAllSuccess<QuerySnapshot>(
                pendingTasksQuery.get(),
                completedTasksQuery.get()
            ).addOnSuccessListener { querySnapshots ->
                taskList.clear()
                querySnapshots.forEach { snapshot ->
                    for (doc in snapshot) {
                        val task = doc.toObject(UserRecentTask::class.java).copy(taskId = doc.id)
                        combinedTaskList.add(task)
                    }
                }
                taskList.addAll(combinedTaskList.sortedByDescending { it.formattedTime })
                adapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                Log.e("UserDashboard", "Failed to fetch tasks", e)
            }
        } ?: run {
            Log.e("UserDashboard", "User ID is null")
        }
    }
}