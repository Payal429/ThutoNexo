package com.example.thutonexo

data class ChatListModel(
    val chatId: String = "",
    val userId: String = "",
    val username: String = "",
    val lastMessage: String = "",
    val timestamp: String = "",
    val profileImageBase64: String = "",
    val unreadCount: Int = 0
)