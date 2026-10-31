package ir.hajarrajaee.booking

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Portable password-protected backup, not a raw Android Keystore dump.
 * AES-256-GCM + PBKDF2-HMAC-SHA256. Losing backup password is unrecoverable.
 */
object SecureBackup {
    private val magic="VYBK1".toByteArray(Charsets.US_ASCII)
    private const val ROUNDS=250_000
    private fun derive(password:CharArray,salt:ByteArray):SecretKeySpec{
        val p=PBEKeySpec(password,salt,ROUNDS,256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(p).encoded,"AES") }
        finally { p.clearPassword() }
    }
    fun export(store:BookingStore,password:String):ByteArray {
        require(password.length>=12){"رمز بکاپ باید حداقل ۱۲ کاراکتر باشد."}
        val root=JSONObject().put("format",1).put("clients",JSONArray()).put("sessions",JSONArray()).put("notes",JSONArray()).put("deletions",JSONArray())
        store.queuedDeletions().forEach{d->root.getJSONArray("deletions").put(JSONObject()
            .put("remote_id",d.remoteId).put("sync_key",d.key).put("version",d.version))}
        val clients=store.clients()
        clients.forEach{ c ->
            root.getJSONArray("clients").put(JSONObject().put("id",c.id).put("name",c.name)
                .put("phone",c.phone).put("history",c.history))
            store.notes(c.id).forEach{n->
                root.getJSONArray("notes").put(JSONObject().put("id",n.id).put("client_id",n.clientId)
                    .put("session_id",n.sessionId?:JSONObject.NULL).put("body",n.body).put("created",n.createdAt))
            }
        }
        store.sessions().forEach{s->
            root.getJSONArray("sessions").put(JSONObject().put("id",s.id).put("client_id",s.clientId)
                .put("start",s.startMillis).put("minutes",s.minutes).put("status",s.status)
                .put("series",s.series?:JSONObject.NULL).put("topic",s.topic)
                .put("sync_key",s.syncKey).put("remote_id",s.remoteId?:JSONObject.NULL)
                .put("remote_version",s.remoteVersion).put("dirty",s.dirty)
                .put("progress",s.progress))
        }
        val salt=ByteArray(16).also{SecureRandom().nextBytes(it)}
        val iv=ByteArray(12).also{SecureRandom().nextBytes(it)}
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE,derive(password.toCharArray(),salt),GCMParameterSpec(128,iv))
            updateAAD(magic)
        }
        return magic+salt+iv+cipher.doFinal(root.toString().toByteArray(Charsets.UTF_8))
    }
    fun restore(store:BookingStore,bytes:ByteArray,password:String):Int {
        require(bytes.size in 50..15_000_000){"فایل بکاپ نامعتبر یا بیش از حد بزرگ است."}
        require(bytes.take(5).toByteArray().contentEquals(magic)){"فرمت فایل قابل شناسایی نیست."}
        val salt=bytes.copyOfRange(5,21)
        val iv=bytes.copyOfRange(21,33)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply{
            init(Cipher.DECRYPT_MODE,derive(password.toCharArray(),salt),GCMParameterSpec(128,iv))
            updateAAD(magic)
        }
        val json=JSONObject(String(cipher.doFinal(bytes.copyOfRange(33,bytes.size)),Charsets.UTF_8))
        require(json.getInt("format")==1){"نسخه بکاپ پشتیبانی نمی‌شود."}
        val clients=json.getJSONArray("clients")
        val sessions=json.getJSONArray("sessions")
        val notes=json.getJSONArray("notes")
        val deletions=json.optJSONArray("deletions") ?: JSONArray()
        require(deletions.length()<=100000)
        require(clients.length()<=20000 && sessions.length()<=100000 && notes.length()<=100000)
        val available=mutableSetOf<Long>()
        for(i in 0 until clients.length()){
            val o=clients.getJSONObject(i)
            require(o.getString("name").isNotBlank() && o.getString("name").length<=160)
            available.add(o.getLong("id"))
        }
        for(i in 0 until sessions.length()){
            val o=sessions.getJSONObject(i)
            require(o.getLong("client_id") in available && o.getInt("minutes") in 5..480)
        }
        for(i in 0 until notes.length()){
            val o=notes.getJSONObject(i)
            require(o.getLong("client_id") in available)
        }
        // All validation and GCM authentication happen before the destructive transaction.
        val db=store.writableDatabase
        db.beginTransaction()
        try {
            db.delete("notes",null,null)
            db.delete("session_progress",null,null)
            db.delete("pending_remote_deletions",null,null)
            db.delete("sessions",null,null)
            db.delete("clients",null,null)
            for(i in 0 until clients.length()){
                val o=clients.getJSONObject(i)
                db.insertOrThrow("clients",null,ContentValues().apply{
                    put("id",o.getLong("id"));put("name",FieldCrypto.encrypt(o.getString("name")))
                    put("phone",FieldCrypto.encrypt(o.optString("phone")))
                    put("history",FieldCrypto.encrypt(o.optString("history")))
                })
            }
            for(i in 0 until sessions.length()){
                val o=sessions.getJSONObject(i)
                db.insertOrThrow("sessions",null,ContentValues().apply {
                    put("id",o.getLong("id"));put("client_id",o.getLong("client_id"))
                    put("start_millis",o.getLong("start"));put("minutes",o.getInt("minutes"))
                    put("status",o.getString("status"))
                    if(!o.isNull("series"))put("series",o.getString("series"))
                    put("topic",FieldCrypto.encrypt(o.optString("topic")))
                    if(!o.isNull("sync_key"))put("sync_key",o.optString("sync_key"))
                    if(!o.isNull("remote_id"))put("remote_id",o.getLong("remote_id"))
                    put("remote_version",o.optInt("remote_version",0))
                    put("dirty",if(o.optBoolean("dirty",true))1 else 0)
                })
                val progress=o.optString("progress","scheduled")
                require(progress in setOf("scheduled","arrived","completed","no_show"))
                if(progress!="scheduled") db.execSQL("INSERT INTO session_progress(session_id,progress) VALUES(?,?)",
                    arrayOf(o.getLong("id"),progress))
            }
            for(i in 0 until notes.length()){
                val o=notes.getJSONObject(i)
                db.insertOrThrow("notes",null,ContentValues().apply{
                    put("id",o.getLong("id"));put("client_id",o.getLong("client_id"))
                    if(!o.isNull("session_id"))put("session_id",o.getLong("session_id"))
                    put("body",FieldCrypto.encrypt(o.getString("body")));put("created_at",o.getLong("created"))
                })
            }
            for(i in 0 until deletions.length()){
                val d=deletions.getJSONObject(i)
                require(d.getLong("remote_id")>0 && d.getInt("version")>0 &&
                    d.getString("sync_key").matches(Regex("[a-f0-9-]{16,64}")))
                db.execSQL("INSERT INTO pending_remote_deletions(remote_id,sync_key,remote_version) VALUES(?,?,?)",
                    arrayOf(d.getLong("remote_id"),d.getString("sync_key"),d.getInt("version")))
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        return sessions.length()
    }
}
