package com.example.chatapp

import android.content.ContentValues
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private val userCollection = Firebase.firestore.collection(USERS_COLLECTION)
    var userDataListener: ListenerRegistration? = null
    var chatListener: ListenerRegistration? = null
    var chats by mutableStateOf<List<ChatData>>(emptyList())

    fun resetState() {

    }

    fun onSignInResult(signInResult: SignInResult) {
        _state.update {
            it.copy(
                isSignedIn = signInResult.data != null,
                signInError = signInResult.errorMessage
            )
        }
    }

    fun adduserToFireStore(userdata: UserData) {
        val userDataMap = mapOf(
            "userId" to userdata.userId,
            "username" to userdata.username,
            "ppurl" to userdata.ppurl,
            "email" to userdata.email
        )
        val userDocument = userCollection.document(userdata.userId)
        userDocument.get().addOnSuccessListener {
            if (it.exists()) {
                userDocument.update(userDataMap).addOnSuccessListener {
                    Log.d(ContentValues.TAG, "User data updated to firebase successfully")
                }.addOnFailureListener {
                    Log.d(ContentValues.TAG, "User data updated to firebase failed")
                }
            } else {
                userDocument.set(userDataMap).addOnSuccessListener {
                    Log.d(ContentValues.TAG, "User data added to firebase successfully")
                }.addOnFailureListener {
                    Log.d(ContentValues.TAG, "User data added to firebase failed")
                }
            }
        }
    }

    fun getUserData(userId: String) {
        userDataListener = userCollection.document(userId).addSnapshotListener { value, error ->
            if (value != null) {
                _state.update {
                    it.copy(userData = value.toObject(UserData::class.java))
                }
            }
        }
    }

    fun hideDialog() {
        _state.update { it.copy(showDialog = false) }
    }

    fun showDialog() {
        _state.update { it.copy(showDialog = true) }
    }

    fun setSrEmail(email: String) {
        _state.update { it.copy(srEmail = email) }

    }

    fun addChat(email: String) {

        Firebase.firestore.collection(CHAT_COLLECTION).where(
            Filter.or(
                Filter.and(
                    Filter.equalTo("user1.email", email),
                    Filter.equalTo("user2.email", state.value.userData?.email)
                ),
                Filter.and(
                    Filter.equalTo("user1.email", state.value.userData?.email),
                    Filter.equalTo("user2.email", email)
                )
            )
        ).get().addOnSuccessListener {
            if (it.isEmpty) {
                userCollection.whereEqualTo("email", email).get().addOnSuccessListener {

                    if (it.isEmpty) {
                        println("Failed")
                    } else {

                        val chatPartner = it.toObjects(UserData::class.java).firstOrNull()
                        val id = Firebase.firestore.collection(CHAT_COLLECTION).document().id
                        val chat = ChatData(
                            chatId = id,
                            last = Message(
                                senderId = "",
                                content = "",
                                time = null
                            ),
                            user1 = ChatUserData(
                                userId = state.value.userData?.userId.toString(),
                                typing = false,
                                bio = state.value.userData?.bio.toString(),
                                username = state.value.userData?.username.toString(),
                                ppurl = state.value.userData?.ppurl.toString(),
                                email = state.value.userData?.email.toString()
                            ),
                            user2 = ChatUserData(
                                bio = chatPartner?.bio.toString(),
                                typing = false,
                                username = chatPartner?.username.toString(),
                                ppurl = chatPartner?.ppurl.toString(),
                                email = chatPartner?.email.toString(),
                                userId = chatPartner?.userId.toString()
                            )
                        )
                        Firebase.firestore.collection(CHAT_COLLECTION).document(id).set(chat)
                    }
                }
            }
        }
    }

    fun showChats(userId: String) {
        chatListener = Firebase.firestore.collection(CHAT_COLLECTION).where(
            Filter.or(
                Filter.equalTo("user1.userId", userId),
                Filter.equalTo("user2.userId", userId)
            )
        ).addSnapshotListener { value, error ->
            if (value != null) {
                chats = value.documents.mapNotNull {
                    it.toObject<ChatData>()
                }.sortedBy {
                    it.last?.time
                }.reversed()
            }
        }
    }
}