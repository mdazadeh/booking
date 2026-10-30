package ir.hajarrajaee.booking

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.time.Instant

private const val REMINDER_CHANNEL = "vaghyar_appointments"
private const val REMINDER_ACTION = "ir.hajarrajaee.booking.REMIND"
private const val EXTRA_ID = "appointment_id"

object ReminderScheduler {
    private const val PREF = "reminder_settings"
    fun enabled(context:Context)=context.getSharedPreferences(PREF,Context.MODE_PRIVATE).getBoolean("enabled",true)
    fun leadMinutes(context:Context)=context.getSharedPreferences(PREF,Context.MODE_PRIVATE).getInt("lead",60)
    fun configure(context:Context,enabled:Boolean,lead:Int) {
        require(lead in 1..1440)
        context.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
            .putBoolean("enabled",enabled).putInt("lead",lead).apply()
        replace(context,emptyList(),BookingStore(context).use { it.sessions() })
    }
    fun channel(context:Context) {
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            REMINDER_CHANNEL,"یادآوری نوبت‌ها",NotificationManager.IMPORTANCE_HIGH).apply {
            description="هشدار شروع جلسه، بدون نمایش اطلاعات خصوصی مراجع در صفحه قفل"
            lockscreenVisibility=android.app.Notification.VISIBILITY_PRIVATE
        })
    }
    private fun pending(context:Context,id:Long):PendingIntent {
        val intent=Intent(context,ReminderReceiver::class.java).apply {
            action=REMINDER_ACTION;putExtra(EXTRA_ID,id)
        }
        return PendingIntent.getBroadcast(context,(id xor (id ushr 32)).toInt(),intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    fun cancel(context:Context,id:Long) {
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending(context,id))
    }
    fun replace(context:Context,old:List<LocalSession>,current:List<LocalSession>) {
        old.forEach{cancel(context,it.id)}
        if(!enabled(context))return
        val alarms=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val lead=leadMinutes(context)
        val now=System.currentTimeMillis()
        current.filter{it.status=="approved"}.forEach {s->
            val trigger=s.startMillis-lead*60_000L
            if(trigger<=now || s.startMillis<=now) return@forEach
            val pi=pending(context,s.id)
            if(Build.VERSION.SDK_INT>=31 && !alarms.canScheduleExactAlarms()) {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi)
            } else {
                try { alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi) }
                catch(_:SecurityException) {alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,trigger,pi)}
            }
        }
    }
    fun refresh(context:Context) {
        BookingStore(context).use { replace(context,it.sessions(),it.sessions()) }
    }
}

class ReminderReceiver:android.content.BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action!=REMINDER_ACTION)return
        val id=intent.getLongExtra(EXTRA_ID,-1)
        if(id<0 || !ReminderScheduler.enabled(context))return
        val session=BookingStore(context).use { db->db.sessions().firstOrNull{it.id==id} }
        if(session==null || session.status!="approved" || session.startMillis<System.currentTimeMillis())return
        if(Build.VERSION.SDK_INT>=33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        ReminderScheduler.channel(context)
        val open=PendingIntent.getActivity(context,0,
            Intent(context,MainActivity::class.java).apply {
                flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification=NotificationCompat.Builder(context,REMINDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("یادآوری نوبت وقت‌یار")
            .setContentText("یک جلسه به‌زودی آغاز می‌شود. برای مشاهده جزئیات وارد برنامه شوید.")
            .setContentIntent(open).setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        NotificationManagerCompat.from(context).notify((id xor (id ushr 32)).toInt(),notification)
    }
}
class ReminderBootReceiver:android.content.BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent) {
        if(intent.action==Intent.ACTION_BOOT_COMPLETED ||
           intent.action=="android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" ||
           intent.action==Intent.ACTION_TIME_CHANGED ||
           intent.action==Intent.ACTION_TIMEZONE_CHANGED) {
            val result=goAsync()
            Thread {
                try{ReminderScheduler.refresh(context.applicationContext)}
                finally{result.finish()}
            }.start()
        }
    }
}
