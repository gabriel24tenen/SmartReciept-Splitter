package com.example.smartreceiptsplitter.domain.model

data class Person(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val avatarPath: String? = null,
    val isSelf: Boolean = false // if True, it the app's own user
)