package ir.hajarrajaee.booking

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64

/** Encrypts PII before SQLite writes. Keys stay in Android Keystore; not backed up. */
object FieldCrypto {
    private const val ALIAS="vaghyar_pii_aes_v1"
    private const val PREFIX="enc:v1:"
    private fun key():SecretKey{
        val store=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        val existing=store.getKey(ALIAS,null) as? SecretKey
        if(existing!=null)return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }
    fun encrypt(value:String):String {
        if(value.isEmpty())return ""
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val blob=cipher.iv+cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return PREFIX+Base64.encodeToString(blob,Base64.NO_WRAP)
    }
    fun decrypt(value:String):String {
        if(!value.startsWith(PREFIX))return value // migration for existing beta plaintext
        val raw=Base64.decode(value.removePrefix(PREFIX),Base64.NO_WRAP)
        require(raw.size>=29){"Encrypted record corrupted"}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,raw.copyOfRange(0,12)))
        return String(cipher.doFinal(raw.copyOfRange(12,raw.size)),Charsets.UTF_8)
    }
}
