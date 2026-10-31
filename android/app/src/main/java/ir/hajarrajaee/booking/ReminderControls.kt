package ir.hajarrajaee.booking

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReminderControls(context:Context) {
    var enabled by remember {mutableStateOf(ReminderScheduler.enabled(context))}
    var lead by remember { mutableIntStateOf(ReminderScheduler.leadMinutes(context)) }
    var status by remember {mutableStateOf("")}
    val askNotifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){
        granted -> status=if(granted)"مجوز اعلان دریافت شد." else "بدون مجوز اعلان، هشدار نمایش داده نمی‌شود."
    }
    Card(shape=RoundedCornerShape(20.dp)){
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("🔔 یادآوری و آلارم جلسات",style=MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("فعال بودن یادآوری")
                Switch(checked=enabled,onCheckedChange={
                    enabled=it;ReminderScheduler.configure(context,enabled,lead)
                })
            }
            Text("مدت قبل از جلسه")
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(10,30,60,120).forEach { min ->
                    FilterChip(selected=lead==min,onClick={
                        lead=min;ReminderScheduler.configure(context,enabled,lead)
                    },label={Text(min.toString().faDigits()+" دقیقه",
                        fontSize=14.sp,fontWeight=FontWeight.Medium)})
                }
            }
            OutlinedButton(onClick={
                if(Build.VERSION.SDK_INT>=33)askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                else status="نسخه اندروید نیازی به مجوز جداگانه اعلان ندارد."
            }){Text("اجازه نمایش اعلان")}
            if(Build.VERSION.SDK_INT>=31) {
                val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if(!alarm.canScheduleExactAlarms()){
                    Text("مجوز آلارم دقیق هنوز فعال نیست؛ هشدار ممکن است با تأخیر برسد.")
                    OutlinedButton(onClick={
                        try{context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))}
                        catch(_:Exception){status="مجوز آلارم از تنظیمات گوشی قابل مدیریت است."}
                    }){Text("تنظیم مجوز آلارم دقیق")}
                } else Text("مجوز آلارم دقیق فعال است.",color=Green)
            }
            if(status.isNotBlank())Text(status,color=Slate)
        }
    }
}
