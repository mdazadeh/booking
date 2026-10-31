package ir.hajarrajaee.booking

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PendingActions
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

val Forest=Color(0xFF20372F)
val Green=Color(0xFF0BA77C)
val Pale=Color(0xFFF7FAF9)
val Slate=Color(0xFF62776E)

@Composable
fun NativeApp(context: Context) {
    val db= remember { BookingStore(context.applicationContext) }
    DisposableEffect(db){ onDispose { db.close() } }
    var signedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var activeDay by remember { mutableStateOf(Jalali.today()) }
    var editSession by remember { mutableStateOf<LocalSession?>(null) }
    var online by remember { mutableStateOf(false) }
    var quickVisible by remember { mutableStateOf(false) }
    var quickDate by remember { mutableStateOf(Jalali.today()) }
    var quickTime by remember { mutableStateOf<LocalTime?>(null) }
    var quickEdit by remember { mutableStateOf<LocalSession?>(null) }
    fun openQuick(date:LocalDate,time:LocalTime?,session:LocalSession?=null){
        quickDate=date;quickTime=time;quickEdit=session;quickVisible=true
    }
    var clock by remember { mutableStateOf(LocalTime.now(Jalali.zone).withNano(0)) }
    LaunchedEffect(Unit) { while (isActive) { clock=LocalTime.now(Jalali.zone).withNano(0); delay(1000L) } }
    val clients=remember(revision){db.clients()}
    val sessions=remember(revision){db.sessions()}
    val scope=rememberCoroutineScope()
    var syncWarning by remember {mutableStateOf("")}
    fun refresh(){
        ReminderScheduler.replace(context,sessions,db.sessions())
        revision++
        if(SyncClient.connected(context))scope.launch{
            try { val report=SyncClient.synchronize(context)
                if(report.conflicts.isNotEmpty())syncWarning="همگام‌سازی تعارض دارد: "+report.conflicts.first()
                revision++
            } catch(e:Exception){syncWarning="همگام‌سازی: "+(e.message?:"خطا")}
        }
    }
    LaunchedEffect(signedIn){
        if(signedIn) {
            ReminderScheduler.refresh(context)
            if(SyncClient.connected(context)) {
                SyncWorker.schedule(context)
                try{
                    val report=SyncClient.synchronize(context)
                    if(report.conflicts.isNotEmpty())syncWarning=report.conflicts.first()
                    revision++
                }catch(e:Exception){syncWarning="همگام‌سازی: "+(e.message?:"خطا")}
            }
        }
    }
    if(!signedIn) {
        Column(Modifier.fillMaxSize().background(Pale).verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement=Arrangement.Center) {
            Card(shape=RoundedCornerShape(30.dp),
                border=BorderStroke(1.dp,Color(0xFFAAFFE4).copy(alpha=0.6f)),
                elevation=CardDefaults.cardElevation(defaultElevation=16.dp),
                colors=CardDefaults.cardColors(containerColor=Color(0xFF073D2C))) {
                Column(Modifier.fillMaxWidth().padding(28.dp)) {
                    Text("وقت‌یار",color=Color.White,fontSize=32.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("مدیریت نوبت‌ها • تقویم شمسی",color=Color(0xFFBEE6CD))
                    Spacer(Modifier.height(18.dp))
                    Text("ورود محلی مدیر",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(username,{username=it},singleLine=true,label={Text("نام کاربری")},
                        colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,
                            unfocusedTextColor=Color.White,unfocusedLabelColor=Color.White,
                            focusedLabelColor=Color.White),
                        modifier=Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(password,{password=it},singleLine=true,
                        visualTransformation=PasswordVisualTransformation(),label={Text("رمز عبور")},
                        colors=OutlinedTextFieldDefaults.colors(focusedTextColor=Color.White,
                            unfocusedTextColor=Color.White,unfocusedLabelColor=Color.White,
                            focusedLabelColor=Color.White),modifier=Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    Button(onClick={
                        if(LocalAuth.login(context,username,password)){
                            signedIn=true;password="";authError=""
                        }else authError="نام کاربری یا رمز عبور صحیح نیست."
                    },modifier=Modifier.fillMaxWidth().height(52.dp),
                        colors=ButtonDefaults.buttonColors(containerColor=Green)) {Text("ورود به تقویم")}
                    if(authError.isNotBlank())Text(authError,color=Color(0xFFFFC8AF),modifier=Modifier.padding(top=8.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("حالت محلی آزمایشی: بدون اینترنت و بدون نیاز به هاست. داده‌های این نسخه در حافظه خصوصی برنامه ذخیره می‌شوند.",
                fontSize=13.sp,color=Slate)
        }
        return
    }
    if(online) {
        Column(Modifier.fillMaxSize()) {
            Button(onClick={online=false},modifier=Modifier.fillMaxWidth().padding(6.dp)){
                Text("بازگشت به مدیریت محلی")
            }
            Box(Modifier.weight(1f)) { BookingScreen(context) }
        }
        return
    }
    Scaffold(
        containerColor=Pale,
        topBar={
            Surface(color=Color.White,shadowElevation=2.dp) {
                Row(Modifier.fillMaxWidth().padding(start=17.dp,end=17.dp,top=14.dp,bottom=14.dp),
                    verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){
                    Column {
                        Text("وقت‌یار",color=Forest,fontSize=25.sp,fontWeight=FontWeight.Bold)
                        Text(Jalali.label(Jalali.today())+" • "+clock.toString().faDigits(),fontSize=13.sp,color=Slate)
                    }
                    Surface(color=Color(0xFFE3F5EC),shape=RoundedCornerShape(16.dp)) {
                        Text("تقویم و نوبت‌دهی",color=Forest,fontSize=12.sp,
                            modifier=Modifier.padding(horizontal=12.dp,vertical=9.dp))
                    }
                }
            }
        },
        bottomBar={
            NavigationBar(containerColor=Color.White) {
                val labels=listOf("تقویم","مراجعان","درخواست‌ها","بیشتر")
                val symbols=listOf(Icons.Outlined.CalendarMonth,Icons.Outlined.Groups,Icons.Outlined.PendingActions,Icons.Outlined.Settings)
                labels.forEachIndexed{i,label->
                    NavigationBarItem(selected=currentPage==i,onClick={currentPage=i},
                        icon={Icon(symbols[i],contentDescription=label)},
                        label={Text(label,fontSize=12.sp)},alwaysShowLabel=true)
                }
            }
        }
    ){ padding ->
        Box(Modifier.fillMaxSize().padding(padding)){
            when(currentPage){
                0->CalendarPanel(activeDay,{activeDay=it},sessions,clients,
                    onEdit={s->openQuick(Jalali.time(s.startMillis).toLocalDate(),
                        Jalali.time(s.startMillis).toLocalTime(),s)},
                    onAdd={date,time->activeDay=date;openQuick(date,time)})
                1->ClientsPanel(db,clients,sessions,{refresh()})
                2->BookingScreen(context)
                else->MorePanel(context,onLogout={signedIn=false},onOpenOnline={currentPage=2},
                    onDataChanged={refresh()})
            }
            if(syncWarning.isNotBlank() && currentPage==3)Text(syncWarning,color=Color(0xFFA34B2D))
        }
    }
    if(quickVisible) QuickBookingSheet(db,clients,sessions,quickDate,quickTime,quickEdit,
        onDismiss={quickVisible=false;quickEdit=null},
        onSaved={quickVisible=false;quickEdit=null;refresh()})
}

@Composable
private fun DashboardPanel(sessions:List<LocalSession>,clients:List<LocalClient>,
    onNew:()->Unit,onCalendar:()->Unit) {
    val today=Jalali.today()
    val active=sessions.filter{it.status=="approved"}
    val todaySessions=active.filter{Jalali.time(it.startMillis).toLocalDate()==today}
    val upcoming=active.filter{Jalali.time(it.startMillis).toLocalDate()>=today}.sortedBy{it.startMillis}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)) {
        Text("داشبورد امروز",fontWeight=FontWeight.Bold,fontSize=23.sp,color=Forest)
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            MetricCard("نوبت‌های امروز",todaySessions.size.toString(),Modifier.weight(1f),Forest,Color.White)
            MetricCard("مراجعان",clients.size.toString(),Modifier.weight(1f),Color(0xFFE1F4E6),Forest)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp),modifier=Modifier.fillMaxWidth()){
            Button(onClick=onNew,modifier=Modifier.weight(1f)){Text("ثبت نوبت جدید")}
            OutlinedButton(onClick=onCalendar,modifier=Modifier.weight(1f)){Text("مشاهده تقویم")}
        }
        Text("جلسات آینده",fontWeight=FontWeight.Bold,fontSize=19.sp,color=Forest)
        if(upcoming.isEmpty())Text("هنوز نوبتی ثبت نشده است.",color=Slate)
        upcoming.take(10).forEach{session->
            val name=clients.firstOrNull{it.id==session.clientId}?.name?:"مراجع"
            SessionCard(session,name,null)
        }
    }
}

@Composable
private fun MetricCard(label:String,value:String,modifier:Modifier,color:Color,textColor:Color){
    Card(modifier=modifier,shape=RoundedCornerShape(22.dp),
        border=BorderStroke(1.dp,Color.White.copy(alpha=0.6f)),
        elevation=CardDefaults.cardElevation(defaultElevation=8.dp),
        colors=CardDefaults.cardColors(containerColor=color.copy(alpha=0.92f))){
        Column(Modifier.padding(18.dp)) {
            Text(label,color=textColor,fontSize=13.sp)
            Text(value,color=textColor,fontSize=32.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
fun SessionCard(s:LocalSession,name:String,onClick:(()->Unit)?) {
    Card(onClick=onClick?:{},enabled=onClick!=null,modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(17.dp),
        border=BorderStroke(1.dp,Color(0xFFCBEBDA)),
        elevation=CardDefaults.cardElevation(defaultElevation=4.dp),
        colors=CardDefaults.cardColors(containerColor=Color.White.copy(alpha=0.88f))){
        Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text(name,fontWeight=FontWeight.Bold,fontSize=16.sp,color=Forest)
                if(s.series!=null)Text("↻ تکرارشونده",fontSize=11.sp,color=Green)
            }
            Text(Jalali.format(s.startMillis),color=Slate,fontSize=13.sp)
            Text("مدت: "+s.minutes.toString().faDigits()+" دقیقه"+(if(s.topic.isBlank())"" else " • "+s.topic),
                color=Slate,fontSize=12.sp)
        }
    }
}

@Composable
private fun MorePanel(context:Context,onLogout:()->Unit,onOpenOnline:()->Unit,onDataChanged:()->Unit) {
    var old by remember{ mutableStateOf("") }
    var new by remember{ mutableStateOf("") }
    var feedback by remember{ mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(17.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات و اتصال",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Forest)
        Card(shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Text("تغییر رمز ورود محلی",fontWeight=FontWeight.Bold)
                OutlinedTextField(old,{old=it},label={Text("رمز فعلی")},modifier=Modifier.fillMaxWidth(),
                    visualTransformation=PasswordVisualTransformation(),singleLine=true)
                OutlinedTextField(new,{new=it},label={Text("رمز جدید (حداقل ۱۰ کاراکتر)")},
                    modifier=Modifier.fillMaxWidth(),visualTransformation=PasswordVisualTransformation(),singleLine=true)
                Button(onClick={
                    feedback=if(LocalAuth.change(context,old,new))"رمز ورود تغییر کرد." else "رمز فعلی اشتباه است یا رمز جدید کوتاه است."
                    old="";new=""
                }){Text("ذخیره رمز جدید")}
                if(feedback.isNotBlank())Text(feedback,color=Slate)
            }
        }
        Card(shape=RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text("رزرو آنلاین مشتری",fontWeight=FontWeight.Bold)
                Text("پس از نصب Backend، اطلاعات نوبت‌ها از بخش «همگام‌سازی با سایت» به سرور ارسال و دریافت می‌شود. اطلاعات خصوصی شرح حال فقط روی گوشی می‌ماند.",
                    fontSize=13.sp,color=Slate)
                OutlinedButton(onClick=onOpenOnline){Text("مدیریت درخواست‌ها و لینک‌های رزرو")}
            }
        }
        ReminderControls(context)
        BackupControls(context,onRestored=onDataChanged)
        ServerControls(context,onDataChanged=onDataChanged)
        Text("حفاظت فیلدهای شخصی با Android Keystore و بکاپ با رمز مجزا اضافه شده است. همگام‌سازی فعلاً فقط نوبت و نام/شماره تماس را پوشش می‌دهد؛ شرح حال و یادداشت‌ها روی گوشی می‌مانند. تا پایان تست امنیتی اطلاعات حساس واقعی ثبت نکنید.",
            fontSize=13.sp,color=Color(0xFF99522A))
        OutlinedButton(onClick=onLogout){Text("خروج از اپلیکیشن")}
    }
}
