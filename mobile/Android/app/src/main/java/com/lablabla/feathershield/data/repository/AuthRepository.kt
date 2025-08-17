package com.lablabla.feathershield.data.repository

import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun createUser(email: String, password: String): AuthResult {
        val authResult = auth.createUserWithEmailAndPassword(email, password).await()
        authResult.user?.let {
            createFirestoreUserDocument(it)
        }
        return authResult
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signInWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = auth.signInWithCredential(credential).await()
        authResult.user?.let {
            createFirestoreUserDocument(it)
        }
        return authResult
    }

    private suspend fun createFirestoreUserDocument(user: FirebaseUser) {
        val userDocument = firestore.collection("users").document(user.uid)
        userDocument.set(mapOf("email" to user.email, "created_at" to System.currentTimeMillis())).await()
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }
}