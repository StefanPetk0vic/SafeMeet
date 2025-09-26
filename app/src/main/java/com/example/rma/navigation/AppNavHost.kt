package com.example.rma

import AuthRepository
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rma.ui.login.HomePage
import com.example.rma.ui.login.LoginScreen
import com.example.rma.ui.login.RegisterScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun AppNavHost(navController: NavHostController, authRepository: AuthRepository) {
    NavHost(navController = navController, startDestination = "login") {


        composable("login") {
            val scope = rememberCoroutineScope()

            LoginScreen(
                onLoginClick = { email, password ->
                    scope.launch {
                        val success = authRepository.login(email, password)
                        if (success) {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            // show Snackbar / Toast
                        }
                    }
                },
                onRegisterClick = {
                    navController.navigate("register")
                }
            )
        }

        composable("register") {
            val scope = rememberCoroutineScope()

            RegisterScreen(
                onRegisterClick = { email, password, fullName, username, phone, photoUri ->
                    scope.launch {
                        val success = authRepository.register(
                            email, password, fullName, username, phone, photoUri
                        )
                        if (success) {
                            navController.navigate("home") {
                                popUpTo("register") { inclusive = true }
                            }
                        } else {
                            // handle error
                        }
                    }
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("home") {
            var profileUrl by remember { mutableStateOf<String?>(null) }

            // Fetch from Firestore once
            LaunchedEffect(authRepository.firebaseAuth.currentUser?.uid) {
                val uid = authRepository.firebaseAuth.currentUser?.uid ?: return@LaunchedEffect
                val doc = authRepository.db.collection("users").document(uid).get().await()
                profileUrl = doc.getString("profilePictureUrl")

                Log.i("PFP","PFP: ${profileUrl}")
            }

            HomePage(
                profilePictureUrl = profileUrl,
                onProfileClick = { /* open profile */ },
                onLiveClick = { /* TODO */ },
                onPinClick = { /* TODO */ },
                onFriendsClick = { /* TODO */ }
            )
        }

    }
}
