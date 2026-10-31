package ir.hajarrajaee.booking

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class OnlineRequest(val id:Long,val name:String,val starts:String,val status:String)
private fun localDateLabel(value:String):String = try {
    Jalali.label(LocalDate.parse(value.take(10)))+" • "+value.substring(11,16).faDigits()
}catch(_:Exception){value.faDigits()}

/** One server login, shared with SyncClient. No second token or remote password storage. */
@Composable
fun RequestsPanel(context:Context,onApproved:()->Unit){
    var connected by remember{mutableStateOf(SyncClient.connected(context))}
    var user by remember{mutableStateOf("admin")}
    var password by remember{mutableStateOf("")}
    var tab by remember{mutableIntStateOf(0)}
    var busy by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf("")}
    var requests by remember{mutableStateOf(listOf<OnlineRequest>())}
    var appointments by remember{mutableStateOf(listOf<OnlineRequest>())}
    var name by remember{mutableStateOf("")}
    var phone by remember{mutableStateOf("")}
    var inviteUrl by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    suspend fun refresh(){
        val data=SyncClient.adminRequest(context,"GET","/admin/overview")
        fun parse(value:JSONArray?):List<OnlineRequest>{
            if(value==null)return emptyList()
            return (0 until value.length()).map{i->
                val row=value.getJSONObject(i)
                OnlineRequest(row.optLong("id"),row.optString("name"),
                    row.optString("starts_at"),row.optString("status"))
            }
        }
        requests=parse(data.optJSONArray("requests"))
        appointments=parse(data.optJSONArray("appointments"))
        message=""
    }
    LaunchedEffect(connected){
        if(connected)try{busy=true;refresh()}catch(e:Exception){
            message="دریافت درخواست‌ها ناموفق بود: "+e.message
        }finally{busy=false}
    }
    Column(Modifier.fillMaxSize().padding(15.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("رزرو آنلاین مشتریان",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Forest)
        if(!connected){
            Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
                Column(Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)){
                    Text("اتصال به سرور وقت‌یار",fontWeight=FontWeight.Bold,color=Forest)
                    Text("یک‌بار وارد شوید؛ درخواست‌ها و همگام‌سازی از همین اتصال استفاده می‌کنند.",
                        color=Slate,fontSize=13.sp)
                    OutlinedTextField(user,{user=it},label={Text("نام کاربری سرور")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    OutlinedTextField(password,{password=it},
                        label={Text("رمز سرور")},visualTransformation=PasswordVisualTransformation(),
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    Button(onClick={scope.launch{
                        busy=true
                        try{SyncClient.login(context,user,password);password="";connected=true;refresh()}
                        catch(e:Exception){message="ورود ناموفق: "+e.message}
                        finally{busy=false}
                    }},enabled=!busy && password.isNotBlank(),
                        modifier=Modifier.fillMaxWidth()){Text("اتصال امن")}
                }
            }
        }else{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                Text("متصل به hajarrajaee.ir",color=Green,fontSize=13.sp)
                TextButton(onClick={scope.launch{
                    busy=true
                    try{refresh()}catch(e:Exception){message="خطا: "+e.message}
                    finally{busy=false}
                }},enabled=!busy){Text("به‌روزرسانی")}
            }
            TabRow(selectedTabIndex=tab){
                listOf("درخواست‌ها","لینک خصوصی","نوبت‌ها").forEachIndexed{i,title->
                    Tab(selected=tab==i,onClick={tab=i},text={Text(title,fontSize=12.sp)})
                }
            }
            when(tab){
                0->{
                    val pending=requests.filter{it.status=="pending"}
                    val others=requests.filter{it.status!="pending"}
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
                        if(pending.isEmpty())item{Text("درخواست جدیدی برای تأیید وجود ندارد.",
                            modifier=Modifier.padding(12.dp),color=Slate)}
                        items(pending,key={it.id}){entry->
                            Card(colors=CardDefaults.cardColors(containerColor=Color.White),
                                shape=RoundedCornerShape(17.dp)){
                                Column(Modifier.fillMaxWidth().padding(15.dp),
                                    verticalArrangement=Arrangement.spacedBy(8.dp)){
                                    Text(entry.name,fontWeight=FontWeight.Bold,color=Forest)
                                    Text(localDateLabel(entry.starts),color=Slate,fontSize=13.sp)
                                    Text("در انتظار تأیید مدیر",color=Color(0xFFB57A2D))
                                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                                        Button(onClick={scope.launch {
                                            busy=true
                                            try{
                                                SyncClient.adminRequest(context,"POST",
                                                    "/admin/requests/"+entry.id+"/approve")
                                                refresh();onApproved()
                                            }catch(e:Exception){message="تأیید ناموفق: "+e.message}
                                            finally{busy=false}
                                        }},enabled=!busy){Text("تأیید")}
                                        OutlinedButton(onClick={scope.launch{
                                            busy=true
                                            try{SyncClient.adminRequest(context,"POST",
                                                "/admin/requests/"+entry.id+"/reject");refresh()}
                                            catch(e:Exception){message="رد ناموفق: "+e.message}
                                            finally{busy=false}
                                        }},enabled=!busy){Text("رد درخواست")}
                                    }
                                }
                            }
                        }
                        if(others.isNotEmpty())item{
                            Text("سوابق اخیر",fontWeight=FontWeight.Bold,color=Forest)
                        }
                        items(others.take(20),key={it.id}){entry->
                            Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
                                Row(Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement=Arrangement.SpaceBetween){
                                    Column {
                                        Text(entry.name,fontWeight=FontWeight.Medium,color=Forest)
                                        Text(localDateLabel(entry.starts),fontSize=12.sp,color=Slate)
                                    }
                                    Text(when(entry.status){
                                        "approved"->"تأییدشده"
                                        "rejected"->"ردشده"
                                        "expired"->"منقضی"
                                        "cancelled"->"لغوشده"
                                        else->entry.status},fontSize=12.sp,color=Slate)
                                }
                            }
                        }
                    }
                }
                1->Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text("لینک اختصاصی فقط برای مراجع موردنظر ارسال می‌شود.",
                        color=Slate,fontSize=13.sp)
                    OutlinedTextField(name,{name=it},label={Text("نام مراجع")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    PhoneNumberField(phone,{phone=it},"موبایل (اختیاری)")
                    Button(onClick={scope.launch{
                        busy=true
                        try{
                            val r=SyncClient.adminRequest(context,"POST","/admin/invitations",
                                JSONObject().put("name",name.trim()).put("phone",canonicalPhone(phone)))
                            inviteUrl=r.getString("url");message=""
                        }catch(e:Exception){message="لینک ساخته نشد: "+e.message}
                        finally{busy=false}
                    }},enabled=!busy && name.isNotBlank(),
                        modifier=Modifier.fillMaxWidth()){Text("ساخت لینک خصوصی")}
                    if(inviteUrl.isNotBlank()){
                        Text(inviteUrl,color=Green,fontSize=12.sp)
                        Button(onClick={
                            val intent=Intent(Intent.ACTION_SEND).apply {
                                type="text/plain";putExtra(Intent.EXTRA_TEXT,inviteUrl)
                            }
                            context.startActivity(Intent.createChooser(intent,"اشتراک لینک رزرو"))
                        },modifier=Modifier.fillMaxWidth()){Text("اشتراک‌گذاری لینک")}
                    }
                }
                else->LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    if(appointments.isEmpty())item{Text("نوبت قطعی جدیدی روی سرور وجود ندارد.",color=Slate)}
                    items(appointments,key={it.id}){entry->
                        Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
                            Column(Modifier.fillMaxWidth().padding(14.dp)){
                                Text(entry.name,fontWeight=FontWeight.Bold,color=Forest)
                                Text(localDateLabel(entry.starts),color=Slate,fontSize=12.sp)
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible=busy,enter=fadeIn(tween(180)),exit=fadeOut(tween(160))){
            VaghLoadingLine("در حال دریافت و پردازش درخواست‌ها...")
        }
        if(message.isNotBlank())Text(message,color=Color(0xFFAD5835),fontSize=12.sp)
    }
}
