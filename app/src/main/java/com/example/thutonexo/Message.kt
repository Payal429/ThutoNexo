package com.example.thutonexo

data class Message(
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0,
    val read: Boolean = false,
    val readAt: Long? = null
)
