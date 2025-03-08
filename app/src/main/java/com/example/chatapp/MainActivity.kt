package com.example.chatapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chatapp.googleSign.GoogleAuthUiClient
import com.example.chatapp.screens.ChatUI
import com.example.chatapp.screens.ChatsScreenUI
import com.example.chatapp.screens.SignInScreenUI
import com.example.chatapp.ui.theme.ChatAppTheme
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ChatViewModel by viewModels()
    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            viewModel = viewModel,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                        //.padding(innerPadding)
                    ) {

                        val state by viewModel.state.collectAsState()
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = StartScreen) {

                            composable<StartScreen> {
                                LaunchedEffect(key1 = Unit) {
                                    val userData = googleAuthUiClient.getSignedInUser()
                                    if (userData != null) {
                                        viewModel.getUserData(userData.userId)
                                        viewModel.showChats(userData.userId)
                                        viewModel.popStory(userData.userId)
                                        navController.navigate(ChatsScreen)
                                    } else {
                                        navController.navigate(SignInScreen)
                                    }
                                }
                            }

                            composable<SignInScreen> {
                                val launcher = rememberLauncherForActivityResult(
                                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                                    onResult = { result ->
                                        if (result.resultCode == RESULT_OK) {
                                            lifecycleScope.launch {
                                                val signInResult =
                                                    googleAuthUiClient.signInWithIntent(
                                                        intent = result.data ?: return@launch
                                                    )
                                                viewModel.onSignInResult(signInResult)
                                            }
                                        }
                                    })

                                LaunchedEffect(key1 = state.isSignedIn) {
                                    val userData = googleAuthUiClient.getSignedInUser()
                                    userData?.run {
                                        viewModel.adduserToFireStore(userData)
                                        viewModel.getUserData(userData.userId)
                                        viewModel.showChats(userData.userId)
                                        viewModel.popStory(userData.userId)
                                        navController.navigate(ChatsScreen)
                                    }
                                }

                                SignInScreenUI(
                                    onSignInClick = {
                                        lifecycleScope.launch {
                                            val signInIntentSender = googleAuthUiClient.signIn()
                                            launcher.launch(
                                                IntentSenderRequest.Builder(
                                                    signInIntentSender ?: return@launch
                                                ).build()
                                            )
                                        }
                                    }
                                )
                            }

                            composable<ChatsScreen> {

                                ChatsScreenUI(
                                    viewModel = viewModel,
                                    state = state,
                                    showSingleChat = { usr, id ->
                                        viewModel.getTp(id)
                                        viewModel.setChatUser(usr, id)
                                        navController.navigate(ChatScreen)
                                    }
                                )
                            }

                            composable<ChatScreen>(
                                enterTransition = {
                                    slideInHorizontally(
                                        initialOffsetX = {
                                            fullWidth -> fullWidth
                                        },
                                        animationSpec = tween(200)
                                    )
                                },
                                exitTransition = {
                                    slideOutHorizontally(
                                        targetOffsetX = {
                                            fullWidth -> fullWidth
                                        },
                                        animationSpec = tween(200)
                                    )
                                }
                            ) {

                                ChatUI(
                                    viewModel = viewModel,
                                    navController = navController,
                                    messages = viewModel.messages,
                                    userData = state.User2!!,
                                    chatId = state.chatId,
                                    state = state,
                                    onBack = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}