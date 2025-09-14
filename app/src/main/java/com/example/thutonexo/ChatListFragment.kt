package com.example.thutonexo

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thutonexo.databinding.FragmentChatListBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class ChatListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private val chatList = mutableListOf<ChatListModel>()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = FirebaseAuth.getInstance().uid!!
    private val userListeners = mutableMapOf<String, ListenerRegistration>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.chatRecyclerView)
        adapter = ChatListAdapter(chatList) { chat ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("chatId", chat.chatId)
            intent.putExtra("receiverId", chat.userId)
            intent.putExtra("receiverName", chat.username)
            startActivity(intent)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadChats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListeners.values.forEach { it.remove() }
    }

    private fun loadChats() {
        // Remove old listeners
        userListeners.values.forEach { it.remove() }
        userListeners.clear()
        chatList.clear()
        adapter.notifyDataSetChanged()

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                for (doc in snapshot.documents) {
                    val chatId = doc.id
                    val participants = doc.get("participants") as? List<String> ?: continue
                    val otherUserId = participants.firstOrNull { it != currentUserId } ?: continue

                    val userRef = db.collection("users").document(otherUserId)

                    // Remove old listener for the same user
                    userListeners[otherUserId]?.remove()

                    val listener = userRef.addSnapshotListener { userDoc, _ ->
                        if (userDoc != null && userDoc.exists()) {
                            val name = userDoc.getString("name") ?: "Unknown"
                            val profileImageBase64 = userDoc.getString("profileImage") ?: ""

                            db.collection("chats").document(chatId)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { messageSnapshot ->
                                    val lastMessageDoc = messageSnapshot.documents.firstOrNull()
                                    val lastMessageText = lastMessageDoc?.getString("text") ?: ""
                                    val timestampText = (lastMessageDoc?.get("timestamp") as? Long)?.let {
                                        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                                    } ?: ""

                                    // 🔹 Now fetch unread count
                                    db.collection("chats").document(chatId)
                                        .collection("messages")
                                        .whereEqualTo("read", false)
                                        .get()
                                        .addOnSuccessListener { snap ->
                                            val totalUnread = snap.documents.count { it.getString("senderId") != currentUserId }

                                            val index = chatList.indexOfFirst { it.userId == otherUserId }
                                            if (index != -1) {
                                                chatList[index] = chatList[index].copy(
                                                    username = name,
                                                    profileImageBase64 = profileImageBase64,
                                                    lastMessage = lastMessageText,
                                                    timestamp = timestampText,
                                                    unreadCount = totalUnread // ✅ update unread count
                                                )
                                                adapter.notifyItemChanged(index)
                                            } else {
                                                chatList.add(
                                                    ChatListModel(
                                                        chatId = chatId,
                                                        userId = otherUserId,
                                                        username = name,
                                                        lastMessage = lastMessageText,
                                                        timestamp = timestampText,
                                                        profileImageBase64 = profileImageBase64,
                                                        unreadCount = totalUnread // ✅ add unread count
                                                    )
                                                )
                                                adapter.notifyDataSetChanged()
                                            }
                                        }
                                }

                        }
                    }

                    userListeners[otherUserId] = listener
                }
            }
    }
}