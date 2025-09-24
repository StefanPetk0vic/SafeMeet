import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Dispatcher
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AuthRepository{
    private val tag = "AuthRepository: "
    private val firebaseAuth = FirebaseAuth.getInstance()

    fun IsLoggedIn():Boolean{
    if(firebaseAuth.currentUser !=null){
        return true
        println(tag + "Aready logged in")
    }
        return false
    }
    suspend fun register(
        email:String,password:String, fullName:String, username:String, phone:String, photoUri: Uri
    ): Boolean{
        try{
            val res = suspendCoroutine { continuation ->
                firebaseAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener {
                    println(tag + "register success")
                    CoroutineScope(Dispatchers.IO).launch {
                        continuation.resume(login(email,password))
                    }
                }.addOnFailureListener {
                    println(tag + "register failure")
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
            println(tag + "register exception ${e.message}")
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
}