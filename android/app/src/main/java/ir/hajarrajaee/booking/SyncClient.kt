package ir.hajarrajaee.booking

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SyncReport(val uploaded:Int,val received:Int,val conflicts:List<String>,val truncated:Boolean)

object SyncClient {
    private val mutex=Mutex()
    private const val PREFERENCES="booking_server_session"
    private val endpoint=BuildConfig.API_BASE_URL
    private fun preferences(context:Context)=context.getSharedPreferences(PREFERENCES,Context.MODE_PRIVATE)
    fun connected(context:Context)=preferences(context).contains("encrypted_token")
    fun disconnect(context:Context)=preferences(context).edit().remove("encrypted_token").apply()
    private fun token(context:Context):String {
        val data=preferences(context).getString("encrypted_token",null)?:error("اتصال به سرور تنظیم نشده است.")
        return FieldCrypto.decrypt(data)
    }
    private suspend fun call(method:String,path:String,auth:String?,body:JSONObject?=null):JSONObject=
        withContext(Dispatchers.IO) {
            val conn=(URL(endpoint+path).openConnection() as HttpURLConnection).apply {
                requestMethod=method
                connectTimeout=12_000;readTimeout=20_000
                setRequestProperty("Accept","application/json")
                if(auth!=null)setRequestProperty("Authorization","Bearer "+auth)
                if(body!=null) {
                    doOutput=true
                    setRequestProperty("Content-Type","application/json; charset=UTF-8")
                    outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
                }
            }
            try{
                val code=conn.responseCode
                val stream=if(code in 200..299)conn.inputStream else conn.errorStream
                val result=JSONObject(stream?.bufferedReader()?.use{it.readText()}?:"{}")
                if(code !in 200..299)throw IllegalStateException(
                    "HTTP "+code+": "+result.optString("error","خطای سرور"))
                result
            }finally{conn.disconnect()}
        }
    suspend fun login(context:Context,user:String,password:String) {
        require(user.isNotBlank() && password.isNotEmpty())
        val result=call("POST","/auth/login",null,
            JSONObject().put("username",user).put("password",password))
        val secret=result.getString("token")
        require(secret.length>=32)
        preferences(context).edit().putString("encrypted_token",FieldCrypto.encrypt(secret)).apply()
    }
    suspend fun synchronize(context:Context):SyncReport=mutex.withLock {
        val auth=token(context)
        withContext(Dispatchers.IO) {
            BookingStore(context).use { db ->
                var uploaded=0
                val conflicts=mutableListOf<String>()
                // Server is authoritative: upload each local edit under server-side day lock.
                db.dirtySessions().forEach { appointment ->
                    val client=db.clients().firstOrNull{it.id==appointment.clientId}
                    if(client==null){
                        conflicts.add("مراجع برای نوبت #"+appointment.id+" یافت نشد")
                        return@forEach
                    }
                    val action=when{
                        appointment.remoteId==null && appointment.status=="cancelled"->{
                            // Never synced before; no server tombstone needed.
                            db.markSynced(appointment.id,0,0);return@forEach
                        }
                        appointment.remoteId==null->"create"
                        appointment.status=="cancelled"->"cancel"
                        else->"update"
                    }
                    val body=JSONObject()
                        .put("action",action)
                        .put("sync_key",appointment.syncKey)
                        .put("remote_id",appointment.remoteId?:JSONObject.NULL)
                        .put("expected_version",appointment.remoteVersion)
                        .put("name",client.name).put("phone",client.phone)
                        .put("starts_ms",appointment.startMillis)
                        .put("minutes",appointment.minutes)
                    try{
                        val result=call("POST","/admin/sync/mutate",auth,body)
                        db.markSynced(appointment.id,result.getLong("remote_id"),result.getInt("version"))
                        uploaded++
                    }catch(e:Exception){
                        // Conflicts never overwrite remote slots; local record remains dirty.
                        conflicts.add(Jalali.format(appointment.startMillis)+": "+(e.message?:"خطای همگام‌سازی"))
                    }
                }
                val snapshot=call("GET","/admin/sync/snapshot",auth)
                val entries=snapshot.getJSONArray("appointments")
                var received=0
                for(i in 0 until entries.length()){
                    val row=entries.getJSONObject(i)
                    val ok=db.importRemote(
                        row.getLong("remote_id"),row.optString("sync_key").takeIf{it.isNotBlank()},
                        row.getInt("version"),row.getString("name"),row.optString("phone"),
                        row.getLong("starts_ms"),row.getInt("minutes"),row.getString("status"))
                    if(ok)received++ else conflicts.add("تعارض محلی با رکورد سرور #"+row.getLong("remote_id"))
                }
                ReminderScheduler.refresh(context)
                SyncReport(uploaded,received,conflicts,snapshot.optBoolean("truncated",false))
            }
        }
    }
}
