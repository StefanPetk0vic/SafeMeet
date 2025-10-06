import android.net.Uri
import android.util.Log
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository{
    private val tag = "AuthRepository"

    val firebaseAuth = FirebaseAuth.getInstance()

    val db = Firebase.firestore

    fun IsLoggedIn():Boolean{
    if(firebaseAuth.currentUser !=null){
        return true
        Log.i(tag,"Aready logged in")
    }
        return false
    }
    suspend fun register(
        email:String,password:String, fullName:String, username:String, phone:String, photoUri: Uri?
    ): Boolean{
        try{
            val res = suspendCoroutine { continuation ->
                firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener { authResult ->
                    Log.i(tag,"register success")

                    var userId = authResult.user?.uid

                    if(userId != null){
                        CoroutineScope(Dispatchers.IO).launch {
                            var uploadedUrl = photoUri?.let { CloudinarySavePicture(it) }
                            uploadedUrl = uploadedUrl?.replace("http://", "https://")

                            Log.d(tag,"userID found")
                            var UserData = hashMapOf(
                            "email" to email,
                            "fullName" to fullName,
                            "username" to username,
                            "phone" to phone,
                            "profilePictureUrl" to (uploadedUrl ?: "")
                            )
                            db.collection("users").document(userId).set(UserData)
                                .addOnSuccessListener {
                                    Log.v(tag, "User data saved to Firestore for $userId")
                                    CoroutineScope(Dispatchers.IO).launch {
                                        continuation.resume(login(email,password))
                                    }
                                }.addOnFailureListener {e ->
                                    Log.e(tag,"Error saving user data to Firestore: ${e.message}")
                                    continuation.resume(false)
                                }
                        }
                    }
                    else{
                        Log.e(tag, "User ID not found after registration!")
                        continuation.resume(false)
                    }
                }.addOnFailureListener {e ->
                    Log.e(tag, "register failure: ${e.message}")
                    continuation.resume(false)
                }
            }
            return res
        } catch(e: Exception){
            e.printStackTrace()
            if(e is CancellationException){
                throw e
            }
            Log.e(tag, "register exception ${e.message}")
            return false
        }
    }


    suspend fun login(email:String,password:String): Boolean{
        try{
            val res = suspendCoroutine { continuation ->
                firebaseAuth.signInWithEmailAndPassword(email,password).addOnSuccessListener {
                    println(tag + "login success")
                    continuation.resume(true)
                }.addOnFailureListener {
                    println(tag + "login failure")
                    continuation.resume(false)
                }
            }
            return res
        }

        catch(e: Exception){
            e.printStackTrace()
            if(e is CancellationException){
                throw e
            }
            println(tag + "login exception ${e.message}")
            return false
        }
    }
    fun logout(){
        firebaseAuth.signOut();
    }

    suspend fun updateUserLocation(lat: Double, lon: Double, isLive:Boolean) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val updates = mapOf(
            "lat" to lat,
            "isLive" to isLive,
            "lon" to lon,
            "lastUpdated" to System.currentTimeMillis()
        )
        db.collection("users")
            .document(userId)
            .update(updates)
            .await()
    }

    fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: throw IllegalStateException("User not logged in")
    }


    suspend fun CloudinarySavePicture(photoUri: Uri):String?{
        return suspendCoroutine { continuation ->
            MediaManager.get().upload(photoUri).callback(object : UploadCallback{
                override fun onStart(requestId: String?) {
                    Log.d("Cloudinary","Upload started: $requestId")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    Log.d("Cloudinary","Progress: $bytes/ $totalBytes")
                }

                override fun onSuccess(requestId: String, resultData: MutableMap<Any?, Any?>) {
                    val uploadedUrl = resultData["url"] as? String
                    Log.d("Cloudinary", "Upload success: $uploadedUrl")
                    continuation.resume(uploadedUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload error: ${error.description}")
                    continuation.resume(null)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.w("Cloudinary", "Upload rescheduled: ${error.description}")
                }

            }).dispatch()
        }
    }

}