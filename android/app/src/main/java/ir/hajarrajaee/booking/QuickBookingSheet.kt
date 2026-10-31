package ir.hajarrajaee.booking

import androidx.compose.foundation.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material.icons.outlined.Schedule

/** Quick add/edit directly from Calendar. Uses existing local database without schema changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickBookingSheet(
    store:BookingStore,clients:List<LocalClient>,sessions:List<LocalSession>,
    date:LocalDate,initialTime:LocalTime?,edit:LocalSession?,
    onDismiss:()->Unit,onSaved:()->Unit
) {
    val context=LocalContext.current
    val key=edit?.id
    var chosenDate by remember(date,key){mutableStateOf(date)}
    var showCalendar by remember(date,key){mutableStateOf(false)}
    var chosen by remember(date,key){mutableStateOf(edit?.clientId)}
    var search by remember(date,key){mutableStateOf("")}
    var newClient by remember(date,key){mutableStateOf(clients.isEmpty())}
    var name by remember(date,key){mutableStateOf("")}
    var phone by remember(date,key){mutableStateOf("")}
    var topic by remember(date,key){mutableStateOf("")}
    var time by remember(date,key){mutableStateOf((initialTime?:LocalTime.of(10,0)).toString().take(5))}
    var duration by remember(date,key){mutableStateOf((edit?.minutes?:60).toString())}
    var repeat by remember(date,key){mutableStateOf(Repeat.NONE)}
    var repeatCount by remember(date,key){mutableStateOf("4")}
    var repeatOpen by remember(date,key){mutableStateOf(false)}
    var customDates by remember(date,key){mutableStateOf(emptySet<LocalDate>())}
    var error by remember(date,key){mutableStateOf("")}
    var confirmDelete by remember{mutableStateOf(false)}
    var progress by remember(date,key){mutableStateOf(edit?.progress ?: "scheduled")}
    val currentClient=clients.firstOrNull{it.id==chosen}
    ModalBottomSheet(onDismissRequest=onDismiss,containerColor=Color(0xFFFDF9FC),
        sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        Column(Modifier.fillMaxWidth().heightIn(max=670.dp).verticalScroll(rememberScrollState())
            .animateContentSize(animationSpec=tween(220))
            .padding(start=18.dp,end=18.dp,bottom=30.dp),
            verticalArrangement=Arrangement.spacedBy(10.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(11.dp)){
                Surface(shape=RoundedCornerShape(15.dp),color=Color(0xFFE7F9F3)){
                    Icon(Icons.Outlined.EventNote,contentDescription=null,tint=Green,
                        modifier=Modifier.padding(10.dp).size(26.dp))
                }
                Column {
                    Text(if(edit==null)"نوبت جدید" else "ویرایش نوبت",fontSize=24.sp,
                        fontWeight=FontWeight.Bold,color=Forest)
                    Text("تاریخ، ساعت و نام مراجع را مشخص کنید",fontSize=13.sp,color=Slate)
                }
            }
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                Text(Jalali.label(chosenDate),color=Slate,fontSize=16.sp)
                TextButton(onClick={showCalendar=!showCalendar}){Text("تغییر تاریخ")}
            }
            AnimatedVisibility(visible=showCalendar,
                enter=fadeIn(tween(180))+expandVertically(tween(180)),
                exit=fadeOut(tween(140))+shrinkVertically(tween(140))){
                Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
                    Column(Modifier.padding(10.dp)){
                        JalaliMonthPicker(chosenDate,{chosenDate=it;showCalendar=false})
                    }
                }
            }
            val window=WorkingHours.forDate(context,chosenDate)
            if(!window.enabled)Text("این روز در تنظیمات، تعطیل است.",color=Color(0xFFB1672C))
            else Text(("ساعات کاری: "+WorkingHours.clock(window.open)+
                " تا "+WorkingHours.clock(window.close)).faDigits(),fontSize=12.sp,color=Slate)
            if(edit==null) {
                Text("مراجع",fontWeight=FontWeight.Bold,color=Forest)
                if(newClient) {
                    OutlinedTextField(name,{name=it},label={Text("نام و نام خانوادگی *")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    PhoneNumberField(phone,{phone=it})
                    if(clients.isNotEmpty())TextButton(onClick={newClient=false}){
                        Text("انتخاب از مراجعان قبلی")
                    }
                } else {
                    OutlinedTextField(search,{search=it},label={Text("جست‌وجوی نام یا موبایل")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    val results=clients.filter{it.name.contains(search,true) ||
                        it.phone.contains(search.asciiDigits(),true)}.take(15)
                    LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        items(results,key={it.id}){c->
                            FilterChip(selected=chosen==c.id,onClick={chosen=c.id;search=c.name},
                                label={Text(c.name)})
                        }
                    }
                    TextButton(onClick={newClient=true;chosen=null}){Text("＋ مراجع جدید")}
                }
            } else Text("مراجع: "+(currentClient?.name?:"مراجع"),fontWeight=FontWeight.Bold,color=Forest)
            Row(verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(7.dp)){
                Icon(Icons.Outlined.Schedule,contentDescription=null,tint=Lilac,
                    modifier=Modifier.size(19.dp))
                Text("ساعت شروع",fontWeight=FontWeight.Bold,color=Forest,fontSize=16.sp)
            }
            val hours=if(window.enabled)(window.open until window.close step 30)
                .map{WorkingHours.clock(it)} else emptyList()
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                items(hours){value->
                    val start=Jalali.millis(chosenDate,LocalTime.parse(value))
                    val length=(duration.toIntOrNull()?:60)*60000L
                    val free=window.allows(LocalTime.parse(value),duration.toIntOrNull()?:60) && sessions.none{s->s.status=="approved" && s.id!=edit?.id &&
                        start<s.startMillis+s.minutes*60000L && start+length>s.startMillis}
                    FilterChip(selected=time==value,onClick={time=value},enabled=free || time==value,
                        label={Text(value.faDigits())})
                }
            }
            Text("ساعت دقیق",fontWeight=FontWeight.Bold,fontSize=16.sp,color=Forest)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
                verticalAlignment=Alignment.CenterVertically){
                OutlinedTextField(
                    value=time.substringBefore(':').faDigits(),
                    onValueChange={value->
                        time=value.asciiDigits().filter{it.isDigit()}.take(2)+":"+
                            time.substringAfter(':',"00")
                    },label={Text("ساعت")},singleLine=true,modifier=Modifier.weight(1f),
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
                Text(":",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Green)
                OutlinedTextField(
                    value=time.substringAfter(':',"00").faDigits(),
                    onValueChange={value->
                        time=time.substringBefore(':')+":"+
                            value.asciiDigits().filter{it.isDigit()}.take(2)
                    },label={Text("دقیقه")},singleLine=true,modifier=Modifier.weight(1f),
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number))
            }
            Text("جداکننده ساعت به‌صورت خودکار درج می‌شود.",fontSize=13.sp,color=Slate)
            Text("مدت جلسه",fontWeight=FontWeight.Bold,color=Forest)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(8.dp)){
                listOf(30,45,60,90).forEach{value->
                    FilterChip(selected=duration==value.toString(),onClick={duration=value.toString()},
                        label={Text(value.toString().faDigits()+" دقیقه",fontSize=14.sp)})
                }
            }
            if(edit!=null){
                Text("وضعیت مراجعه",fontWeight=FontWeight.Bold,color=Forest)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    listOf("scheduled" to "برنامه‌ریزی‌شده","arrived" to "حاضر شد",
                        "completed" to "انجام شد","no_show" to "حضور نداشت").forEach{(code,label)->
                        FilterChip(selected=progress==code,onClick={
                            try{store.setProgress(edit.id,code);progress=code;onSaved()}
                            catch(e:Exception){error=e.message?:"خطا در ذخیره وضعیت"}
                        },label={Text(label,fontSize=13.sp)})
                    }
                }
            }
            if(edit==null){
                OutlinedTextField(topic,{topic=it},label={Text("توضیح کوتاه (اختیاری)")},
                    modifier=Modifier.fillMaxWidth(),maxLines=3)
                TextButton(onClick={repeatOpen=!repeatOpen}){
                    Text(if(repeatOpen)"بستن گزینه‌های تکرار" else "↻ تکرار نوبت (اختیاری)")
                }
                AnimatedVisibility(visible=repeatOpen,
                    enter=fadeIn(tween(180))+expandVertically(tween(180)),
                    exit=fadeOut(tween(140))+shrinkVertically(tween(140))){
                    Column(verticalArrangement=Arrangement.spacedBy(7.dp)){
                        listOf(Repeat.NONE,Repeat.WEEKLY,Repeat.BIWEEKLY,Repeat.MONTHLY,Repeat.CUSTOM)
                            .forEach{option->
                            Row(verticalAlignment=Alignment.CenterVertically){
                                RadioButton(selected=repeat==option,onClick={repeat=option})
                                Text(if(option==Repeat.CUSTOM)"چند تاریخ دلخواه (ساعت یکسان)"
                                    else option.label,Modifier.clickable{repeat=option})
                            }
                        }
                        if(repeat==Repeat.CUSTOM){
                            Text("روزهای دلخواه را در تقویم انتخاب کنید؛ همه با ساعت همین فرم ثبت می‌شوند.",
                                color=Slate,fontSize=13.sp)
                            Card(shape=RoundedCornerShape(19.dp),
                                colors=CardDefaults.cardColors(containerColor=Color.White)){
                                Column(Modifier.padding(10.dp)){
                                    JalaliMonthPicker(chosenDate,onSelect={},
                                        custom=customDates+chosenDate,
                                        onToggleCustom={day->
                                            if(!day.isBefore(chosenDate)&&day!=chosenDate){
                                                customDates=if(day in customDates)customDates-day
                                                    else customDates+day
                                            }
                                        })
                                }
                            }
                            Text(((customDates.filter{!it.isBefore(chosenDate)}.size+1).toString().faDigits())+
                                " تاریخ انتخاب شده",fontSize=14.sp,color=Green)
                        }else if(repeat!=Repeat.NONE){
                            OutlinedTextField(repeatCount.faDigits(),{
                                repeatCount=it.asciiDigits().filter(Char::isDigit).take(3)},
                                modifier=Modifier.fillMaxWidth(),
                                label={Text("تعداد نوبت‌ها؛ حداکثر ۱۰۴")},
                                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
                                singleLine=true)
                        }
                    }
                }
            }
            if(error.isNotBlank())Text(error,color=Color(0xFFB74435))
            Button(onClick={
                try{
                    val normalizedTime=time.substringBefore(':').padStart(2,'0')+":"+
                        time.substringAfter(':',"").padStart(2,'0')
                    val start=LocalTime.parse(normalizedTime)
                    val length=duration.toIntOrNull()?:0
                    require(length in 5..480){"مدت جلسه باید بین ۵ تا ۴۸۰ دقیقه باشد."}
                    val count=when(repeat){
                        Repeat.NONE->1
                        Repeat.CUSTOM->customDates.count{!it.isBefore(chosenDate)}+1
                        else->repeatCount.toIntOrNull()?:0
                    }
                    require(count in 1..104){"تعداد نوبت‌ها باید بین ۱ تا ۱۰۴ باشد."}
                    val dates=if(edit==null)recurrenceDates(chosenDate,repeat,count,customDates)
                        else listOf(chosenDate)
                    val moving=edit==null || Jalali.millis(chosenDate,start)!=edit.startMillis ||
                        length!=edit.minutes
                    if(moving) require(dates.all{WorkingHours.validate(context,it,start,length)}){
                        "زمان انتخابی خارج از ساعات کاری است."
                    }
                    if(edit==null){
                        if(newClient){
                            require(name.trim().isNotEmpty()){"نام مراجع را وارد کنید."}
                            store.createClientAndSessions(name,canonicalPhone(phone),chosenDate,
                                start,length,repeat,count,customDates,topic)
                        }else{
                            val clientId=chosen?:throw IllegalArgumentException("مراجع را انتخاب کنید.")
                            store.addSessions(clientId,chosenDate,start,length,repeat,count,customDates,topic)
                        }
                    }else store.editSession(edit,Jalali.millis(chosenDate,start),length,false)
                    onSaved()
                }catch(e:Exception){error=e.message?:"ذخیره ناموفق بود."}
            },enabled=edit!=null || (if(newClient)name.isNotBlank() else chosen!=null),
                modifier=Modifier.fillMaxWidth().height(53.dp),
                shape=RoundedCornerShape(19.dp)){
                Text(if(edit==null)"ثبت نوبت" else "ذخیره تغییرات",fontSize=16.sp)
            }
            if(edit!=null)OutlinedButton(onClick={confirmDelete=true},modifier=Modifier.fillMaxWidth()){
                Text("لغو نوبت",color=Color(0xFFB74435))
            }
        }
    }
    if(confirmDelete && edit!=null)AlertDialog(
        onDismissRequest={confirmDelete=false},title={Text("لغو نوبت؟")},
        text={Text("این نوبت از تقویم حذف می‌شود.")},
        confirmButton={Button(onClick={store.deleteSession(edit,false);confirmDelete=false;onSaved()}){Text("تأیید لغو")}},
        dismissButton={TextButton(onClick={confirmDelete=false}){Text("بازگشت")}}
    )
}
