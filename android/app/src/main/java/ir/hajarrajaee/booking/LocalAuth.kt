package ir.hajarrajaee.booking

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local test-only password gate, no authentication against hosting.
 * A hard-coded verifier in a public APK is recoverable; do NOT rely on it to protect
 * actual clinical records. Production needs Keystore-backed encryption and stronger access policy.
 */
object LocalAuth {
    private const val DEFAULT_SALT="9d4e0d33569f6618726628624af713d9"
    private const val DEFAULT_HASH="4e979e2dd006910111b4c9fb22cf101e3b01e51561d7f032dd2c460b6392822e"
    private fun bytes(hex:String)=hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private fun hex(bytes:ByteArray)=bytes.joinToString("") { "%02x".format(it) }
    private fun hash(password:String,salt:String):ByteArray {
        val spec=PBEKeySpec(password.toCharArray(),bytes(salt),180000,256)
        return try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded}
        finally{spec.clearPassword()}
    }
    fun login(context:Context,user:String,pass:String):Boolean {
        if(user.trim()!="admin"||pass.isEmpty())return false
        val p=context.getSharedPreferences("booking_local_auth",Context.MODE_PRIVATE)
        val salt=p.getString("salt",DEFAULT_SALT) ?: DEFAULT_SALT
        val expected=p.getString("hash",DEFAULT_HASH) ?: DEFAULT_HASH
        return MessageDigest.isEqual(hash(pass,salt),bytes(expected))
    }
    fun change(context:Context,old:String,new:String):Boolean {
        if(!login(context,"admin",old)||new.length<10)return false
        val salt=ByteArray(16);SecureRandom().nextBytes(salt)
        val p=context.getSharedPreferences("booking_local_auth",Context.MODE_PRIVATE)
        p.edit().putString("salt",hex(salt)).putString("hash",hex(hash(new,hex(salt)))).apply()
        return true
    }
}
