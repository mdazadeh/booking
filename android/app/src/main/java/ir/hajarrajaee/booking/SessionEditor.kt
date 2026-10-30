package ir.hajarrajaee.booking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun SessionEditor(
    store:BookingStore,clients:List<LocalClient>,sessions:List<LocalSession>,
    edit:LocalSession?,onEditClear:()->Unit,onChange:()->Unit,onSelectEdit:(LocalSession)->Unit,onGoClients:()->Unit
) {
    var clientId by remember{mutableStateOf<Long?>(null)}
    var day by remember{mutableStateOf(Jalali.today())}
    var time by remember{mutableStateOf("10:00")}
    var minutes by remember{mutableStateOf("60")}
    var topic by remember{mutableStateOf("")}
    var repeat by remember{mutableStateOf(Repeat.NONE)}
    var count by remember{mutableStateOf("8")}
    var custom by remember{mutableStateOf(setOf<LocalDate>())}
    var search by remember{mutableStateOf("")}
    var allFuture by remember{mutableStateOf(false)}
    var feedback by remember{mutableStateOf("")}
    var deleting by remember{mutableStateOf<LocalSession?>(null)}
    var deleteFuture by remember{mutableStateOf(false)}
    fun reset(){
        clientId=null;day=Jalali.today();time="10:00";minutes="60"
        topic="";repeat=Repeat.NONE;count="8";custom=emptySet();search="";allFuture=false
        onEditClear()
    }
    LaunchedEffect(edit?.id){
        if(edit!=null) {
            val dateTime=Jalali.time(edit.startMillis)
            clientId=edit.clientId;day=dateTime.toLocalDate()
            time=dateTime.toLocalTime().toString()
            minutes=edit.minutes.toString();topic=edit.topic
            repeat=Repeat.NONE;custom=emptySet();allFuture=false
        }
    }
    val active=sessions.filter{it.status=="approved"}.sortedBy{it.startMillis}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(15.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text(if(edit==null)"ثبت نوبت" else "ویرایش نوبت",fontSize=22.sp,
                fontWeight=FontWeight.Bold,color=Forest)
            if(edit!=null)TextButton(onClick={reset()}){Text("نوبت جدید")}
        }
        if(clients.isEmpty()) {
            Card(shape=RoundedCornerShape(16.dp)){
                Column(Modifier.padding(15.dp)){
                    Text("برای ثبت نوبت ابتدا یک پرونده مراجع ایجاد کنید.",color=Slate)
                    Button(onClick=onGoClients){Text("+ افزودن مراجع")}
                }
            }
        } else Card(shape=RoundedCornerShape(20.dp)){
            Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                Text("۱. انتخاب مراجع",fontWeight=FontWeight.Bold,fontSize=17.sp)
                if(edit==null) {
                    OutlinedTextField(search,{search=it},label={Text("جست‌وجوی مراجع")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                }
                val found=clients.filter{it.name.contains(search,true)||it.phone.contains(search,true)}
                LazyRow(horizontalArrangement=Arrangement.spacedBy(7.dp)){
                    items(found,key={it.id}){c->
                        FilterChip(selected=clientId==c.id,onClick={if(edit==null){clientId=c.id}else {}},
                            label={Text(c.name)})
                    }
                }
                Text("۲. انتخاب روز (تقویم کامل جلالی)",fontWeight=FontWeight.Bold,fontSize=17.sp)
                JalaliMonthPicker(day,onSelect={day=it},sessions=sessions)
                Text("۳. تعیین ساعت و مدت دقیق",fontWeight=FontWeight.Bold,fontSize=17.sp)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    OutlinedTextField(time,{time=it},label={Text("شروع HH:mm")},
                        singleLine=true,modifier=Modifier.weight(1f))
                    OutlinedTextField(minutes,{minutes=it.filter{ch->ch.isDigit()}},
                        label={Text("دقیقه")},singleLine=true,modifier=Modifier.weight(1f))
                }
                val start=runCatching{LocalTime.parse(time)}.getOrNull()
                val duration=minutes.toIntOrNull()
                if(start!=null && duration!=null && duration>0)Text(
                    "پایان: "+start.plusMinutes(duration.toLong()).toString(),
                    fontSize=13.sp,color=Green)
                Text("می‌توانید ساعت شناور مثل ۱۷:۴۰ را وارد کنید؛ زمان‌ها به ساعت‌های رند محدود نیستند.",
                    fontSize=12.sp,color=Slate)
                OutlinedTextField(topic,{topic=it},label={Text("موضوع / توضیح کوتاه جلسه")},
                    modifier=Modifier.fillMaxWidth(),minLines=2)
                if(edit==null){
                    Text("۴. تکرار نوبت",fontWeight=FontWeight.Bold,fontSize=17.sp)
                    Column(verticalArrangement=Arrangement.spacedBy(4.dp)){
                        Repeat.entries.chunked(3).forEach{chunk->
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                                chunk.forEach{option->
                                    FilterChip(selected=repeat==option,
                                        onClick={repeat=option},label={Text(option.label,fontSize=11.sp)})
                                }
                            }
                        }
                    }
                    if(repeat!=Repeat.NONE && repeat!=Repeat.CUSTOM) {
                        OutlinedTextField(count,{
                            count=it.filter{c->c.isDigit()}.take(3)
                        },singleLine=true,label={Text("تعداد جلسات (حداکثر ۱۰۴)")},
                            modifier=Modifier.fillMaxWidth())
                    }
                    if(repeat==Repeat.CUSTOM) {
                        Text("تاریخ‌های انتخابی را از تقویم زیر اضافه یا حذف کنید.",
                            color=Slate,fontSize=12.sp)
                        JalaliMonthPicker(day,{day=it},custom=custom,
                            onToggleCustom={d->custom=if(d in custom)custom-d else custom+d})
                        Text("تعداد تاریخ‌های انتخاب‌شده: "+(custom+day).size,fontSize=12.sp)
                    }
                    val n=if(repeat==Repeat.CUSTOM)(custom+day).size else count.toIntOrNull()?:1
                    val sample=recurrenceDates(day,repeat,n,custom)
                    if(repeat!=Repeat.NONE && sample.isNotEmpty()){
                        Text("پیش‌نمایش نوبت‌های تکراری: "+sample.size+" جلسه",
                            color=Green,fontWeight=FontWeight.Bold)
                        Text(sample.take(5).joinToString("، "){Jalali.label(it)}+
                            if(sample.size>5)" ..." else "",fontSize=12.sp,color=Slate)
                    }
                }else if(edit.series!=null){
                    Row(Modifier.fillMaxWidth(),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                        Checkbox(checked=allFuture,onCheckedChange={allFuture=it})
                        Text("اعمال تغییر زمان روی این جلسه و تمام جلسات آینده",
                            modifier=Modifier.weight(1f),fontSize=13.sp)
                    }
                }
                Button(onClick={
                    try {
                        val start=LocalTime.parse(time)
                        if(time.length!=5)throw IllegalArgumentException("ساعت را به شکل HH:mm وارد کنید.")
                        val n=minutes.toIntOrNull()?:0
                        if(n !in 5..480)throw IllegalArgumentException("مدت جلسه باید ۵ تا ۴۸۰ دقیقه باشد.")
                        val chosen=clientId?:throw IllegalArgumentException("مراجع را انتخاب کنید.")
                        if(edit==null) {
                            val number=if(repeat==Repeat.CUSTOM)(custom+day).size else count.toIntOrNull()?:1
                            val done=store.addSessions(chosen,day,start,n,repeat,number,custom,topic)
                            feedback=done.toString()+" نوبت ذخیره شد."
                        } else {
                            store.editSession(edit,Jalali.millis(day,start),n,allFuture)
                            feedback="نوبت با موفقیت ویرایش شد."
                        }
                        reset();onChange()
                    } catch(e:Exception) { feedback=e.message?:"خطا در ذخیره نوبت" }
                },enabled=clients.isNotEmpty(),modifier=Modifier.fillMaxWidth().height(51.dp)){
                    Text(if(edit==null)"ثبت نوبت" else "ذخیره تغییرات")
                }
                if(edit!=null)OutlinedButton(onClick={
                    deleting=edit;deleteFuture=false
                },modifier=Modifier.fillMaxWidth()) {Text("حذف نوبت",color=androidx.compose.ui.graphics.Color(0xFFB74435))}
                if(feedback.isNotBlank())Text(feedback,color=Green,fontSize=13.sp)
            }
        }
        Text("فهرست تمام نوبت‌ها",fontWeight=FontWeight.Bold,fontSize=18.sp,color=Forest)
        if(active.isEmpty())Text("نوبتی ثبت نشده است.",color=Slate)
        active.sortedByDescending{it.startMillis}.take(120).forEach{s->
            val name=clients.firstOrNull{it.id==s.clientId}?.name?:"مراجع"
            SessionCard(s,name){ onSelectEdit(s) }
        }
    }
    deleting?.let{target->
        AlertDialog(onDismissRequest={deleting=null},
            title={Text("حذف نوبت؟")},
            text={
                Column {
                    Text("این عملیات قابل بازگردانی نیست.")
                    if(target.series!=null)Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically){
                        Checkbox(checked=deleteFuture,onCheckedChange={deleteFuture=it})
                        Text("این جلسه و تمام جلسات آینده")
                    }
                }
            },
            confirmButton={Button(onClick={
                store.deleteSession(target,deleteFuture)
                deleting=null;feedback="نوبت حذف شد.";reset();onChange()
            }){Text("حذف نهایی")}},
            dismissButton={TextButton(onClick={deleting=null}){Text("انصراف")}}
        )
    }
}
