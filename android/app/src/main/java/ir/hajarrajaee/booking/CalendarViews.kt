package ir.hajarrajaee.booking

import androidx.compose.foundation.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.EventAvailable
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun JalaliMonthPicker(
    selected:LocalDate,
    onSelect:(LocalDate)->Unit,
    sessions:List<LocalSession> = emptyList(),
    custom:Set<LocalDate> = emptySet(),
    onToggleCustom:((LocalDate)->Unit)?=null
){
    var showing by remember { mutableStateOf(Jalali.monthStart(selected)) }
    LaunchedEffect(selected){
        if(Jalali.jalali(showing).year!=Jalali.jalali(selected).year ||
           Jalali.jalali(showing).month!=Jalali.jalali(selected).month)
            showing=Jalali.monthStart(selected)
    }
    Column(verticalArrangement=Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            IconButton(onClick={showing=Jalali.monthStart(Jalali.addMonth(showing,-1))}){Text("‹",fontSize=27.sp)}
            Text(Jalali.monthLabel(showing),fontSize=17.sp,fontWeight=FontWeight.Bold,color=Forest)
            IconButton(onClick={showing=Jalali.monthStart(Jalali.addMonth(showing,1))}){Text("›",fontSize=27.sp)}
        }
        Row(Modifier.fillMaxWidth()) {
            Jalali.weekdays.forEachIndexed { index,name->
                Text(name.take(3),modifier=Modifier.weight(1f),
                    fontSize=13.sp,textAlign=TextAlign.Center,
                    color=if(index==6)Color(0xFFB05C4C)else Slate)
            }
        }
        val first=Jalali.monthStart(showing)
        val start=first.minusDays(Jalali.weekday(first).toLong())
        val month=Jalali.jalali(showing).month
        (0..5).forEach { row ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                (0..6).forEach { col ->
                    val day=start.plusDays((row*7+col).toLong())
                    val j=Jalali.jalali(day)
                    val inMonth=j.month==month
                    val isSelected=day==selected
                    val isCustom=day in custom
                    val busy=sessions.count { Jalali.time(it.startMillis).toLocalDate()==day && it.status=="approved" }
                    val color=when {
                        isCustom->Color(0xFF1E7650)
                        isSelected->Forest
                        day==Jalali.today()->Color(0xFFE0F3E7)
                        else->Color.White
                    }
                    Surface(onClick={
                        if(onToggleCustom!=null)onToggleCustom(day) else onSelect(day)
                    },modifier=Modifier.weight(1f).height(53.dp),
                        color=color,shape=RoundedCornerShape(11.dp),
                        border=if(day==Jalali.today()&&!isSelected) BorderStroke(1.dp,Green) else null){
                        Column(horizontalAlignment=Alignment.CenterHorizontally,
                            verticalArrangement=Arrangement.Center) {
                            Text(j.day.toString().faDigits(),fontSize=15.sp,
                                fontWeight=if(isSelected)FontWeight.Bold else FontWeight.Normal,
                                color=if(isSelected||isCustom)Color.White else
                                    if(inMonth)Forest else Color(0xFFB4BDB6))
                            if(busy>0) {
                                Spacer(Modifier.height(3.dp))
                                Box(Modifier.size(5.dp).background(if(isSelected||isCustom) Color.White else Green,
                                    RoundedCornerShape(4.dp)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWeekStrip(
    selected:LocalDate,
    onSelect:(LocalDate)->Unit,
    sessions:List<LocalSession>
){
    val start=selected.minusDays(Jalali.weekday(selected).toLong())
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Text("انتخاب روز",fontWeight=FontWeight.Bold,color=Forest,fontSize=15.sp)
            Row(verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick={onSelect(selected.minusWeeks(1))}){
                    Text("‹",fontSize=25.sp,color=Lilac)
                }
                IconButton(onClick={onSelect(selected.plusWeeks(1))}){
                    Text("›",fontSize=25.sp,color=Lilac)
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
            (0..6).forEach { i->
                val day=start.plusDays(i.toLong())
                val active=day==selected
                val count=sessions.count{s->
                    s.status=="approved" && Jalali.time(s.startMillis).toLocalDate()==day
                }
                Surface(onClick={onSelect(day)},modifier=Modifier.weight(1f).height(78.dp),
                    color=if(active)Green else if(i%2==0)Color(0xFFF5EDFE)
                        else Color(0xFFF0FAF8),
                    shape=RoundedCornerShape(17.dp)){
                    Column(Modifier.fillMaxSize(),
                        horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center){
                        Text(Jalali.weekdays[i].take(2),fontSize=11.sp,
                            color=if(active)Color.White else Slate)
                        Text(Jalali.jalali(day).day.toString().faDigits(),
                            fontSize=20.sp,fontWeight=FontWeight.Bold,
                            color=if(active)Color.White else Forest)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.size(5.dp).background(
                            if(active)Color.White else if(count>0)Coral else Color.Transparent,
                            RoundedCornerShape(3.dp)))
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarPanel(
    selected:LocalDate,
    onSelect:(LocalDate)->Unit,
    sessions:List<LocalSession>,
    clients:List<LocalClient>,
    onEdit:(LocalSession)->Unit,
    onAdd:(LocalDate,LocalTime?)->Unit
) {
    val context=androidx.compose.ui.platform.LocalContext.current
    var mode by remember {mutableIntStateOf(2)}
    val window=WorkingHours.forDate(context,selected)
    val all=sessions.filter{
        Jalali.time(it.startMillis).toLocalDate()==selected && it.status=="approved"
    }.sortedBy{it.startMillis}
    val clientNames=remember(clients){clients.associate{it.id to it.name}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        .animateContentSize(animationSpec=tween(230))
        .padding(horizontal=14.dp,vertical=13.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Column {
                Text("برنامه نوبت‌ها",fontSize=24.sp,fontWeight=FontWeight.Bold,color=Forest)
                Crossfade(targetState=selected,animationSpec=tween(200),label="تغییر روز"){
                    shownDay->Text(Jalali.label(shownDay),color=Slate,fontSize=15.sp)
                }
            }
            FilledTonalButton(onClick={onSelect(Jalali.today())},
                shape=RoundedCornerShape(15.dp),
                colors=ButtonDefaults.filledTonalButtonColors(
                    containerColor=Color(0xFFEDE4FB),contentColor=Lilac)){
                Icon(Icons.Outlined.CalendarMonth,contentDescription=null,
                    modifier=Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("امروز",fontWeight=FontWeight.Bold)
            }
        }
        Surface(color=Color(0xFFF5EDFF),shape=RoundedCornerShape(18.dp)){
            Row(Modifier.fillMaxWidth().padding(horizontal=15.dp,vertical=10.dp),
                verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.SpaceBetween){
                Column {
                    Text("برای امروز برنامه‌ریزی کن ✨",fontSize=15.sp,
                        fontWeight=FontWeight.Bold,color=Forest)
                    Text("نوبت‌های این روز: "+all.size.toString().faDigits(),
                        fontSize=13.sp,color=Slate)
                }
                Surface(color=Color.White,shape=RoundedCornerShape(14.dp)){
                    Icon(Icons.Outlined.EventAvailable,contentDescription=null,
                        tint=Lilac,modifier=Modifier.padding(12.dp).size(24.dp))
                }
            }
        }
        SingleChoiceSegmentedButtonRow(modifier=Modifier.fillMaxWidth()){
            listOf("ماهانه","هفتگی","روزانه").forEachIndexed{i,label->
                SegmentedButton(selected=mode==i,onClick={mode=i},
                    shape=SegmentedButtonDefaults.itemShape(index=i,count=3)){Text(label)}
            }
        }
        Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Crossfade(targetState=mode,animationSpec=tween(210),label="نمای تقویم"){shownMode->
                when(shownMode){
                    0->JalaliMonthPicker(selected,onSelect,sessions)
                    1->{
                        val start=selected.minusDays(Jalali.weekday(selected).toLong())
                        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                            horizontalArrangement=Arrangement.SpaceBetween){
                            IconButton(onClick={onSelect(selected.minusWeeks(1))}){Text("‹",fontSize=24.sp)}
                            Text("هفته "+Jalali.label(start),color=Forest,fontWeight=FontWeight.Bold)
                            IconButton(onClick={onSelect(selected.plusWeeks(1))}){Text("›",fontSize=24.sp)}
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(3.dp)){
                            (0..6).forEach{i->
                                val day=start.plusDays(i.toLong())
                                val count=sessions.count{it.status=="approved" &&
                                    Jalali.time(it.startMillis).toLocalDate()==day}
                                Surface(onClick={onSelect(day)},modifier=Modifier.weight(1f).height(77.dp),
                                    color=if(day==selected)Green else Pale,
                                    shape=RoundedCornerShape(11.dp)){
                                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.Center){
                                        Text(Jalali.weekdays[i].take(3),fontSize=10.sp,
                                            color=if(day==selected)Color.White else Slate)
                                        Text(Jalali.jalali(day).day.toString().faDigits(),
                                            fontSize=16.sp,fontWeight=FontWeight.Bold,
                                            color=if(day==selected)Color.White else Forest)
                                        if(count>0)Text(count.toString().faDigits()+" نوبت",
                                            color=if(day==selected)Color.White else Green,fontSize=9.sp)
                                    }
                                }
                            }
                        }
                    }
                    else->CompactWeekStrip(selected,onSelect,sessions)
                }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Column {
                Text("برنامه "+Jalali.weekdays[WorkingHours.dayIndex(selected)],
                    fontWeight=FontWeight.Bold,fontSize=17.sp,color=Forest)
                Text(all.size.toString().faDigits()+" نوبت • "+
                    (if(window.enabled) (WorkingHours.clock(window.open)+" تا "+WorkingHours.clock(window.close)).faDigits()
                    else "روز تعطیل"),fontSize=12.sp,color=Slate)
            }
            Button(onClick={onAdd(selected,null)},shape=RoundedCornerShape(17.dp)){
                Icon(Icons.Outlined.AddCircleOutline,contentDescription=null,
                    modifier=Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text("نوبت جدید",fontWeight=FontWeight.Bold)
            }
        }
        if(mode==2 && window.enabled){
            DayTimeline(selected,all,clientNames,window,onAdd,onEdit)
        }else if(mode==2 && !window.enabled){
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF4E8))){
                Text("این روز در برنامه کاری تعطیل است. ساعات کاری را از تنظیمات تغییر دهید.",
                    modifier=Modifier.fillMaxWidth().padding(17.dp),color=Color(0xFF865D29))
            }
        }
        if(mode!=2 || !window.enabled){
            if(all.isEmpty())Card(colors=CardDefaults.cardColors(containerColor=Color.White)){
                Text("برای این روز هنوز نوبتی ثبت نشده است.",color=Slate,
                    modifier=Modifier.fillMaxWidth().padding(18.dp))
            }else all.forEach{s->
                TimelineEvent(s,clientNames[s.clientId]?:"مراجع",{onEdit(s)})
            }
        }
        Text("برای دیدن نوبت‌ها روز را لمس کنید؛ روی نوبت بزنید تا تغییر زمان یا وضعیت آن باز شود.",
            fontSize=12.sp,color=Slate)
    }
}

@Composable
private fun TimelineEvent(s:LocalSession,name:String,onClick:()->Unit){
    val tint=when(s.progress){
        "arrived"->Color(0xFF127CAC)
        "completed"->Color(0xFF617668)
        "no_show"->Color(0xFFBD6842)
        else->Green
    }
    val progress=when(s.progress){
        "arrived"->"حاضر شد"
        "completed"->"انجام شد"
        "no_show"->"حضور نداشت"
        else->"تأییدشده"
    }
    Surface(onClick=onClick,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),
        color=tint.copy(alpha=.10f),border=BorderStroke(1.dp,tint.copy(alpha=.28f))){
        Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(9.dp)){
            Box(Modifier.width(4.dp).height(38.dp).background(tint,RoundedCornerShape(3.dp)))
            Column(Modifier.weight(1f)){
                Text(name,color=Forest,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text((Jalali.time(s.startMillis).toLocalTime().toString().take(5)+
                    " • "+s.minutes+" دقیقه").faDigits(),color=Slate,fontSize=13.sp)
            }
            Text(progress,color=tint,fontSize=12.sp,fontWeight=FontWeight.Bold)
        }
    }
}

/**
 * Non-overlapping, measured rows: each time bucket grows to fit all appointments.
 * No absolute offsets or minimum-height collision. Historical conflicts remain visible.
 */
@Composable
private fun DayTimeline(
    day:LocalDate,
    entries:List<LocalSession>,
    names:Map<Long,String>,
    window:WorkWindow,
    onAdd:(LocalDate,LocalTime?)->Unit,
    onEdit:(LocalSession)->Unit
){
    val all=entries.sortedWith(compareBy<LocalSession>{it.startMillis}.thenBy{it.id})
    val conflicts=all.flatMapIndexed{i,s->
        all.drop(i+1).filter{other->
            s.startMillis<other.startMillis+other.minutes*60000L &&
                other.startMillis<s.startMillis+s.minutes*60000L
        }.flatMap{listOf(s.id,it.id)}
    }.toSet()
    if(conflicts.isNotEmpty()){
        Surface(color=Color(0xFFFFE7E5),shape=RoundedCornerShape(18.dp),
            modifier=Modifier.fillMaxWidth()){
            Row(Modifier.padding(14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
                Icon(androidx.compose.material.icons.Icons.Outlined.WarningAmber,
                    contentDescription=null,tint=Color(0xFFB34F45))
                Column {
                    Text("تداخل در نوبت‌های موجود",color=Color(0xFF963E3B),
                        fontWeight=FontWeight.Bold,fontSize=15.sp)
                    Text("نوبت‌ها جدا نمایش داده می‌شوند؛ برای اصلاح، هر کارت را لمس کنید.",
                        color=Color(0xFF963E3B),fontSize=12.sp)
                }
            }
        }
    }
    val buckets=(window.open until window.close step 30).toList()
    Column(verticalArrangement=Arrangement.spacedBy(5.dp)){
        buckets.forEach{minute->
            val bucketEnd=minute+30
            val byStart=all.filter{s->
                val t=Jalali.time(s.startMillis).toLocalTime()
                val start=t.hour*60+t.minute
                start>=minute && start<bucketEnd
            }
            val slot=LocalTime.of(minute/60,minute%60)
            val startMs=Jalali.millis(day,slot)
            // Offer a slot only when a standard 60-minute visit can fit.
            val free=window.allows(slot,60) && all.none{s->
                startMs<s.startMillis+s.minutes*60000L &&
                    startMs+60*60000L>s.startMillis
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp),
                verticalAlignment=Alignment.Top){
                Text(WorkingHours.clock(minute).faDigits(),fontSize=13.sp,
                    fontWeight=FontWeight.SemiBold,color=Slate,
                    modifier=Modifier.width(54.dp).padding(top=15.dp))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                    byStart.forEach{s->
                        Column {
                            if(s.id in conflicts)Text("⚠ تداخل زمان",
                                color=Color(0xFFB34F45),fontSize=12.sp,fontWeight=FontWeight.Bold)
                            TimelineEvent(s,names[s.clientId]?:"مراجع"){onEdit(s)}
                        }
                    }
                    if(byStart.isEmpty()) {
                        Surface(onClick={onAdd(day,slot)},enabled=free,
                            modifier=Modifier.fillMaxWidth().heightIn(min=44.dp),
                            shape=RoundedCornerShape(13.dp),
                            color=if(free)Color(0xFFE9F8F2) else Color(0xFFF3F4F8),
                            border=BorderStroke(1.dp,if(free)Color(0xFFB6E5D3) else Color(0xFFE8E8EE))){
                            Row(Modifier.padding(horizontal=14.dp,vertical=12.dp),
                                verticalAlignment=Alignment.CenterVertically,
                                horizontalArrangement=Arrangement.spacedBy(7.dp)){
                                if(free) {
                                    Icon(androidx.compose.material.icons.Icons.Outlined.AddCircleOutline,
                                        contentDescription=null,tint=Green,
                                        modifier=Modifier.size(18.dp))
                                    Text("زمان آزاد · ثبت نوبت",color=Forest,fontSize=14.sp,
                                        fontWeight=FontWeight.Medium)
                                }else {
                                    Text("رزرو شده",color=Slate,fontSize=12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        val offHours=all.filter{s->
            val t=Jalali.time(s.startMillis).toLocalTime()
            val minute=t.hour*60+t.minute
            minute<window.open || minute>=window.close
        }
        if(offHours.isNotEmpty()){
            Text("نوبت‌های خارج از ساعت کاری",fontWeight=FontWeight.Bold,color=Forest)
            offHours.forEach{s->TimelineEvent(s,names[s.clientId]?:"مراجع"){onEdit(s)}}
        }
    }
}
