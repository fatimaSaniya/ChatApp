package com.example.chatapp

import android.content.ContentValues
import android.icu.util.Calendar
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.google.firebase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private val _state = MutableStateFlow(AppState())
    val state = _state.asStateFlow()
    private val userCollection = Firebase.firestore.collection(USERS_COLLECTION)
    var userDataListener: ListenerRegistration? = null
    var chatListener: ListenerRegistration? = null
    var chats by mutableStateOf<List<ChatData>>(emptyList())
    var tp by mutableStateOf(ChatData())
    var tpListener: ListenerRegistration? = null
    var reply by mutableStateOf("")
    var msgListener: ListenerRegistration? = null
    var messages by mutableStateOf<List<Message>>(listOf())
    var storyListener: ListenerRegistration? = null
    var stories by mutableStateOf<List<Story>>(emptyList())

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

    fun getTp(chatId: String) {
        tpListener?.remove()
        tpListener = Firebase.firestore.collection(CHAT_COLLECTION).document(chatId)
            .addSnapshotListener { snp, err ->
                if (snp != null) {
                    tp = snp.toObject(ChatData::class.java)!!
                }
            }
    }

    fun setChatUser(usr: ChatUserData, id: String) {
        _state.update {
            it.copy(User2 = usr, chatId = id)
        }
    }

    fun sendReply(
        chatId: String,
        replyMessage: Message = Message(),
        msg: String,
        senderId: String = state.value.userData?.userId.toString(),
    ) {
        val id = Firebase.firestore.collection(CHAT_COLLECTION).document()
            .collection(MESSAGES_COLLECTION).document().id

        val time = Calendar.getInstance().time
        val message = Message(
            msgId = id,
            repliedMessage = replyMessage,
            senderId = senderId,
            content = msg,
            time = Timestamp(date = time)
        )
        Firebase.firestore.collection(CHAT_COLLECTION).document(chatId)
            .collection(MESSAGES_COLLECTION).document(id).set(message)
        Firebase.firestore.collection(CHAT_COLLECTION).document(chatId).update("last", message)
    }

    fun popMessage(chatId: String) {
        msgListener?.remove()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                if (chatId != "") {
                    msgListener = Firebase.firestore.collection(CHAT_COLLECTION).document(chatId)
                        .collection(MESSAGES_COLLECTION).addSnapshotListener { value, error ->
                            if (value != null) {
                                messages = value.documents.mapNotNull {
                                    it.toObject(Message::class.java)
                                }.sortedBy {
                                    it.time
                                }.reversed()
                            }
                            Log.d("TAG1", "popMessage: $messages")
                        }
                }
            }
        }
    }

    fun uploadImage(img: Uri, callBack: (String) -> Unit) {
        var storageRef = Firebase.storage.reference
        val imageRef = storageRef.child("$IMAGE_COLLECTION/${System.currentTimeMillis()}")
        imageRef.putFile(img).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener {
                val url = it.toString()
                callBack(url)
            }.addOnFailureListener {
                callBack("")
            }
        }.addOnFailureListener {
            callBack("")
        }.addOnCompleteListener {

        }
    }

    fun uploadStory(url: String, storyId: String) {
        val image = Image(
            imgUrl = url,
            time = Timestamp(Calendar.getInstance().time)
        )
        if(storyId.isNotBlank()){
            Firebase.firestore.collection(STORIES_COLLECTION).document(storyId).update("images", FieldValue.arrayUnion(image))
        }else{
            val id = Firebase.firestore.collection(STORIES_COLLECTION).document().id
            val story = Story(
                id = id,
                userId = state.value.userData?.userId.toString(),
                userName = state.value.userData?.username,
                ppUrl = state.value.userData?.ppurl.toString(),
                images = listOf(image)
            )
            Firebase.firestore.collection(STORIES_COLLECTION).document(id).set(story)
        }
    }

    fun popStory(currentUserId: String) {
        viewModelScope.launch {
            val storyCollection = Firebase.firestore.collection(STORIES_COLLECTION)
            val users = arrayListOf(state.value.userData?.userId)
            Firebase.firestore.collection(CHAT_COLLECTION).where(
                Filter.or(
                    Filter.equalTo("user1.userId", currentUserId),
                    Filter.equalTo("user2.userId", currentUserId)
                )
            ).addSnapshotListener { snp, err ->
                if (snp != null) {
                    snp.toObjects<ChatData>().forEach {
                        val otherUserId = if (it.user1?.userId != currentUserId) {
                            it.user2?.userId.toString()
                        } else it.user1.userId.toString()
                        users.add(otherUserId)
                    }
                    users.add(currentUserId)
                    storyListener = storyCollection.whereIn("userId", users)
                        .addSnapshotListener { storySnapShot, storyError ->
                            if (storySnapShot != null) {
                                stories = storySnapShot.documents.mapNotNull {
                                    it.toObject<Story>()
                                }
                            }
                        }
                }
            }
        }
    }
}