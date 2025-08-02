package com.aak.remotepresence.Authentication.Admin.AdminAdapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aak.remotepresence.Authentication.Admin.AdminFragment.NotificationFragment
import com.aak.remotepresence.Authentication.Admin.AdminFragment.UsersFragment

class AdminUserPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UsersFragment()
            1 -> NotificationFragment()
            else -> Fragment()
        }
    }
}
