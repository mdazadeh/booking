package ir.hajarrajaee.booking

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Deliberately device-only, never issues destructive server API requests. */
@Composable
fun LocalResetControls(context:Context,onReset:()->Unit) {
    var open by remember {mutableStateOf(false)}
    var phrase by remember{mutableStateOf("")}
    var backedUp by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    Surface(color=Color(0xFFFFEEF1),shape=RoundedCornerShape(23.dp)){
        Column(Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Icon(Icons.Outlined.RestartAlt,contentDescription=null,tint=Color(0xFFC15470))
                Text("بازنشانی اطلاعات گوشی",style=MaterialTheme.typography.titleMedium,
                    color=Color(0xFF913D59))
            }
            Text("پرونده‌ها، نوبت‌ها و یادداشت‌های محلی حذف می‌شوند. سرور پاک نمی‌شود و اتصال آن قطع خواهد شد.",
                color=Color(0xFF80536A),style=MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()){
                Text("نمایش گزینه ریست")
            }
            if(message.isNotBlank())Text(message,color=Color(0xFF913D59))
        }
    }
    if(open)AlertDialog(
        onDismissRequest={open=false;phrase="";backedUp=false},
        title={Text("ریست کامل اطلاعات این گوشی")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("این عملیات برگشت‌ناپذیر است. همه مراجعان و جلسات محلی پاک می‌شوند. اطلاعات سایت حذف نمی‌شوند.")
            Text("حتماً ابتدا از بخش «بکاپ رمزگذاری‌شده» یک فایل سالم تهیه کنید.")
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                Checkbox(checked=backedUp,onCheckedChange={backedUp=it})
                Text("بکاپ اطلاعاتم را تهیه کرده‌ام.")
            }
            OutlinedTextField(phrase,{phrase=it},singleLine=true,
                label={Text("برای تأیید، «ریست» را بنویسید")})
        }},
        confirmButton={Button(
            onClick={
                try{
                    val db=BookingStore(context)
                    val old=db.sessions()
                    db.clearLocalData()
                    db.close()
                    ReminderScheduler.replace(context,old,emptyList())
                    SyncClient.disconnect(context)
                    context.getSharedPreferences("vaghyar_working_hours_v1",Context.MODE_PRIVATE)
                        .edit().clear().apply()
                    phrase="";backedUp=false;open=false
                    message="اطلاعات محلی پاک و اتصال سرور قطع شد."
                    onReset()
                }catch(e:Exception){message=e.message?:"ریست ناموفق بود."}
            },enabled=backedUp && phrase.trim()=="ریست",
            colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFC15470))){
            Text("تأیید نهایی و ریست")
        }},
        dismissButton={TextButton(onClick={open=false;phrase="";backedUp=false}){
            Text("انصراف")
        }}
    )
}
