package com.aak.remotepresence.Authentication.User

import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.aak.remotepresence.Authentication.User.UserFragment.UserAboutFragment
import com.aak.remotepresence.Authentication.User.UserFragment.UserCreateNewTaskFragment
import com.aak.remotepresence.Authentication.User.UserFragment.UserDashboardFragment
import com.aak.remotepresence.Authentication.User.UserFragment.UserMyTaskFragment
import com.aak.remotepresence.Authentication.User.UserFragment.UserProfileFragment
import com.aak.remotepresence.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class UserDashboardActivity : AppCompatActivity() {
    private lateinit var bottomNavigationView: BottomNavigationView
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_dashboard)

        val userDashboardFragment = UserDashboardFragment()
        val bundle = Bundle()
        bundle.putString("userId", userId)
        userDashboardFragment.arguments = bundle

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, userDashboardFragment)
            .commit()

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        userId = intent.getStringExtra("userId")
        val userType = intent.getStringExtra("userType")
        val username = intent.getStringExtra("username")

        Toast.makeText(this, "Logged in as User: $username", Toast.LENGTH_SHORT).show()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(UserDashboardFragment(), userId, false)
        }
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> loadFragment(UserDashboardFragment(), userId, true)
                R.id.mytask -> loadFragment(UserMyTaskFragment(), userId, true)
                R.id.newtask -> loadFragment(UserCreateNewTaskFragment(), userId, true)
                R.id.about -> loadFragment(UserAboutFragment(), userId, true)
                R.id.profile -> loadFragment(UserProfileFragment(), userId, true)


            }
            true
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                if (currentFragment is UserMyTaskFragment ||
                    currentFragment is UserCreateNewTaskFragment ||
                    currentFragment is UserAboutFragment ||
                    currentFragment is UserProfileFragment
                ) {
                    loadFragment(UserDashboardFragment(), userId, false)
                    bottomNavigationView.selectedItemId = R.id.home
                } else if (currentFragment is UserDashboardFragment) {
                    finish() // Exit app
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

    }

    private fun loadFragment(fragment: Fragment, userId: String?, withAnimation: Boolean) {
        val bundle = Bundle().apply {
            putString("userId", userId)
        }
        fragment.arguments = bundle

        val transaction = supportFragmentManager.beginTransaction()

        if (withAnimation) {
            transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        }

        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

}