package com.example.cabsharing

import androidx.compose.runtime.Composable
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

@Composable
fun FirebaseAuthentication() {
    lateinit var auth: FirebaseAuth
    // Initialize Firebase Auth
    auth = Firebase.auth

}