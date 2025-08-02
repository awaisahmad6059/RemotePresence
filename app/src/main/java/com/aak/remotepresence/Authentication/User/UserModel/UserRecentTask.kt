package com.aak.remotepresence.Authentication.User.UserModel


data class UserRecentTask(
    val taskId: String = "",
    val title: String = "",
    val category: String = "",
    val detail: String = "",
    val location: String = "",
    val status: String = "",
    val formattedTime: String = "",
    val timestamp: Long = 0L,
    val userId: String = ""
)
