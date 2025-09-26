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
import com.example.rma.ui.login.LoginScreen
import com.example.rma.ui.login.RegisterScreen
import com.example.rma.ui.theme.RMATheme
import kotlinx.coroutines.launch
import androidx.navigation.compose.rememberNavController



//CLOUDINARY
import android.net.Uri
import com.bumptech.glide.Glide
import com.cloudinary.Transformation
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

import androidx.appcompat.app.AppCompatActivity

import android.util.Log
import androidx.navigation.compose.rememberNavController

import java.util.HashMap
import java.util.Map

class MainActivity : ComponentActivity() {

    private val cloudName = BuildConfig.CLOUD_NAME
    val apiKey = BuildConfig.CLOUD_API_KEY
    val apiSecret = BuildConfig.CLOUD_API_SECRET


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            AppNavHost(navController, AuthRepository())
        }
    }
}

@SuppressLint("CoroutineCreationDuringComposition")
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RMATheme {
        var authRepository = AuthRepository()
        var showLogin by rememberSaveable { mutableStateOf(authRepository.IsLoggedIn()) }
        var coroutineScope = rememberCoroutineScope()

        if (showLogin) {
            LoginScreen(
                onLoginClick = { email, password -> coroutineScope.launch {
                    var success = authRepository.login(email, password)
                    if(success){
                        showLogin = true
                    }
                    else{
                        throw error("Error while logging in")
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