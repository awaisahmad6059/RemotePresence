package com.aak.remotepresence.Authentication.Admin.AdminFragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.aak.remotepresence.Authentication.Admin.AdminCompleteorderActivity
import com.aak.remotepresence.Authentication.Admin.AdminInprogreeActivity
import com.aak.remotepresence.Authentication.Admin.AdminNewOrderActivity
import com.aak.remotepresence.Authentication.LoginActivity
import com.aak.remotepresence.R
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var menuButton: ImageView
    private lateinit var totalPendingRequestTextView: TextView
    private lateinit var totalinprogresstask: TextView
    private lateinit var total_complete_order_count: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
        view.findViewById<LinearLayout>(R.id.pendingrequest).setOnClickListener {
            startActivity(Intent(requireActivity(), AdminNewOrderActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.inprogresstask).setOnClickListener {
            startActivity(Intent(requireActivity(), AdminInprogreeActivity::class.java))
        }
        view.findViewById<LinearLayout>(R.id.completerequests).setOnClickListener {
            startActivity(Intent(requireActivity(), AdminCompleteorderActivity::class.java))
        }
        drawerLayout = view.findViewById(R.id.drawer_layout)
        navView = view.findViewById(R.id.nav_view)
        menuButton = view.findViewById(R.id.menu_button)
        totalPendingRequestTextView = view.findViewById(R.id.total_pending_request_count)
        totalinprogresstask = view.findViewById(R.id.totalinprogresstask)
        total_complete_order_count = view.findViewById(R.id.total_complete_order_count)


        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }


        // Handle nav drawer items
        navView.setNavigationItemSelectedListener(this)

        fetchAdminDetails(view)
        fetchTotalOrdersCount()
        fetchTotalInProgressCount()
        fetchTotalCompleteCount()
        return view
    }

    private fun fetchTotalCompleteCount() {
        val db = FirebaseFirestore.getInstance()
        db.collection("completeOrders")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val totalTask = snapshot?.size() ?: 0
                total_complete_order_count.text = totalTask.toString()
            }
    }

    private fun fetchTotalInProgressCount(){
        val db = FirebaseFirestore.getInstance()
        db.collection("tasks")
            .whereEqualTo("status", "InProgress")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val totalinprogress = snapshot?.size() ?: 0
                totalinprogresstask.text = totalinprogress.toString()
            }
    }

    private fun fetchTotalOrdersCount() {
        val db = FirebaseFirestore.getInstance()
        db.collection("tasks")
            .whereEqualTo("status", "pending") // Filter to only "Pending" orders
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                val totalPending = snapshot?.size() ?: 0
                totalPendingRequestTextView.text = totalPending.toString()
            }
    }


    private fun fetchAdminDetails(view: View) {
        val db = FirebaseFirestore.getInstance()
        val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("admins")
            .document(adminId)
            .addSnapshotListener { document, error ->
                val userNameTextView = view.findViewById<TextView>(R.id.user_name)

                if (error != null) {
                    userNameTextView.text = "Admin"
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val name = document.getString("username")
                    userNameTextView.text = name ?: "Admin"
                } else {
                    userNameTextView.text = "Admin"
                }
            }
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_signout -> {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            requireActivity().onBackPressed()
        }
    }


}