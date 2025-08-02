package com.aak.remotepresence.Authentication.User.UserFragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aak.remotepresence.Authentication.User.UserAdapter.UserRecentTaskAdapter
import com.aak.remotepresence.Authentication.User.UserModel.UserRecentTask
import com.aak.remotepresence.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class UserAllTasksFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserRecentTaskAdapter
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
        val view = inflater.inflate(R.layout.fragment_user_all_tasks, container, false)

        recyclerView = view.findViewById(R.id.allTasksRecycler)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = UserRecentTaskAdapter(taskList) { task ->
        }
        recyclerView.adapter = adapter

        fetchAllTasks()

        return view
    }

    private fun fetchAllTasks() {
        userId?.let { uid ->
            firestore.collection("tasks")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "pending")
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
