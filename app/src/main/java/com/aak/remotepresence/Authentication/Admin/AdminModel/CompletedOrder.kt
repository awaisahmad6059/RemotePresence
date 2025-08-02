package com.aak.remotepresence.Authentication.Admin.AdminModel

data class CompletedOrder(
    val orderId: String = "",
    val userId: String = "",
    val category: String = "",
    val detail: String = "",
    val instructions: String = "",
    val location: String = "",
    val urgency: String = "",
    val status: String = "",
    var username: String = "",
    var profileImageUrl: String = "",
    var mediaUri: String = ""
)