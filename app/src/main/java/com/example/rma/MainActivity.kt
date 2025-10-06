package com.example.rma
import AuthRepository
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import com.example.rma.ui.login.LoginScreen
import com.example.rma.ui.login.RegisterScreen
import com.example.rma.ui.theme.RMATheme
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController
import com.example.rma.navigation.AppNavHost
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RMATheme {
                val navController = rememberNavController()
                AppNavHost(navController, AuthRepository())
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // App went to background, set isLive = false
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        CoroutineScope(Dispatchers.IO).launch {
            Firebase.firestore
                .collection("users")
                .document(currentUser.uid)
                .update("isLive", false)
        }
    }
}


@SuppressLint("CoroutineCreationDuringComposition")
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RMATheme {
        val authRepository = AuthRepository()
        var showLogin by rememberSaveable { mutableStateOf(authRepository.IsLoggedIn()) }
        val coroutineScope = rememberCoroutineScope()

        if (showLogin) {
            LoginScreen(
                onLoginClick = { email, password -> coroutineScope.launch {
                    val success = authRepository.login(email, password)
                    if(success){
                        showLogin = true
                    }
                }
                },
                onRegisterClick = {
                    showLogin = false
                }
            )
        }else{
                RegisterScreen(
                    onRegisterClick ={email,password, fullName, username, phone, photoUri ->
                        coroutineScope.launch {
                            authRepository.register(email,password, fullName, username, phone, photoUri)
                        }
                    },
                    onBackToLoginClick = {
                        showLogin=true
                    }
                )
            }
        }

    }