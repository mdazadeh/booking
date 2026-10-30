package ir.hajarrajaee.booking

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ServerControls(context:Context,onDataChanged:()->Unit){
    var user by remember{mutableStateOf("admin")}
    var pass by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var connected by remember{mutableStateOf(SyncClient.connected(context))}
    val scope=rememberCoroutineScope()
    Card(shape=RoundedCornerShape(20.dp)){
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("همگام‌سازی با سایت",style=MaterialTheme.typography.titleMedium)
            Text("سرور اصلی نوبت‌های قطعی روی hajarrajaee.ir است. تا قبل از نصب Backend، اتصال فعال نمی‌شود. پرونده و یادداشت خصوصی فعلاً فقط روی گوشی می‌ماند.",
                color=Slate)
            if(!connected) {
                OutlinedTextField(user,{user=it},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    label={Text("نام کاربری مدیر سرور")})
                OutlinedTextField(pass,{pass=it},singleLine=true,modifier=Modifier.fillMaxWidth(),
                    label={Text("رمز سرور")},visualTransformation=PasswordVisualTransformation())
                Button(onClick={scope.launch {
                    busy=true
                    try{SyncClient.login(context,user,pass);pass="";connected=true;message="اتصال برقرار شد."
                        val report=SyncClient.synchronize(context)
                        onDataChanged()
                        message=summary(report)
                    }catch(e:Exception){message="خطای اتصال: "+(e.message?:"نامشخص")}
                    busy=false
                }},enabled=!busy && pass.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("اتصال امن به سرور")}
            } else {
                Button(onClick={scope.launch{
                    busy=true
                    try{val report=SyncClient.synchronize(context);onDataChanged();message=summary(report)}
                    catch(e:Exception){message="همگام‌سازی انجام نشد: "+e.message}
                    busy=false
                }},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("همگام‌سازی اکنون")}
                OutlinedButton(onClick={SyncClient.disconnect(context);connected=false;message="اتصال سرور قطع شد."}){
                    Text("قطع اتصال")
                }
            }
            if(message.isNotBlank())Text(message,color=Slate)
        }
    }
}
private fun summary(report:SyncReport):String = buildString {
    append("ارسال: "+report.uploaded+" • دریافت: "+report.received)
    if(report.truncated)append("\nهشدار: فهرست سرور محدود شده است.")
    if(report.conflicts.isNotEmpty())append("\nتعارض‌ها:\n"+report.conflicts.take(7).joinToString("\n"))
}
