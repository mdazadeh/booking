package ir.hajarrajaee.booking

import androidx.compose.foundation.*
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
                    fontSize=11.sp,textAlign=TextAlign.Center,
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
                            Text(j.day.toString().faDigits(),fontSize=13.sp,
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
        .padding(horizontal=14.dp,vertical=13.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.SpaceBetween){
            Column {
                Text("تقویم نوبت‌ها",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Forest)
                Text(Jalali.label(selected),color=Slate,fontSize=13.sp)
            }
            TextButton(onClick={onSelect(Jalali.today())}){Text("امروز")}
        }
        SingleChoiceSegmentedButtonRow(modifier=Modifier.fillMaxWidth()){
            listOf("ماهانه","هفتگی","روزانه").forEachIndexed{i,label->
                SegmentedButton(selected=mode==i,onClick={mode=i},
                    shape=SegmentedButtonDefaults.itemShape(index=i,count=3)){Text(label)}
            }
        }
        Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
            Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                when(mode){
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
                    else-> Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.SpaceBetween){
                        IconButton(onClick={onSelect(selected.minusDays(1))}){Text("‹",fontSize=25.sp)}
                        Text(Jalali.label(selected),fontWeight=FontWeight.Bold,color=Forest)
                        IconButton(onClick={onSelect(selected.plusDays(1))}){Text("›",fontSize=25.sp)}
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
            Button(onClick={onAdd(selected,null)},shape=RoundedCornerShape(13.dp)){
                Text("＋ نوبت جدید")
            }
        }
        if(mode==2 && window.enabled){
            DayTimeline(selected,all,clientNames,window,onAdd,onEdit)
            val outside=all.filter{session->
                val t=Jalali.time(session.startMillis).toLocalTime()
                val minute=t.hour*60+t.minute
                minute<window.open || minute>=window.close
            }
            if(outside.isNotEmpty()){
                Text("نوبت‌های خارج از ساعات فعلی",fontWeight=FontWeight.Bold,color=Slate)
                outside.forEach{s->TimelineEvent(s,clientNames[s.clientId]?:"مراجع"){onEdit(s)}}
            }
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
        color=tint.copy(alpha=.11f),border=BorderStroke(1.dp,tint.copy(alpha=.36f))){
        Row(Modifier.fillMaxWidth().padding(11.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(9.dp)){
            Box(Modifier.width(4.dp).height(38.dp).background(tint,RoundedCornerShape(3.dp)))
            Column(Modifier.weight(1f)){
                Text(name,color=Forest,fontSize=16.sp,fontWeight=FontWeight.Bold)
                Text((Jalali.time(s.startMillis).toLocalTime().toString().take(5)+
                    " • "+s.minutes+" دقیقه").faDigits(),color=Slate,fontSize=12.sp)
            }
            Text(progress,color=tint,fontSize=11.sp,fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun DayTimeline(
    day:LocalDate,
    entries:List<LocalSession>,
    names:Map<Long,String>,
    window:WorkWindow,
    onAdd:(LocalDate,LocalTime?)->Unit,
    onEdit:(LocalSession)->Unit
){
    val start=window.open
    val end=window.close
    val ppm=1.30f
    val height=((end-start)*ppm).dp
    val rangeSlots=(start until end step 30).toList()
    Surface(shape=RoundedCornerShape(19.dp),color=Color.White) {
        Box(Modifier.fillMaxWidth().height(height).padding(horizontal=10.dp)){
            rangeSlots.forEach {minute->
                val slotStart=LocalTime.of(minute/60,minute%60)
                val withinBreak=window.breakTo>window.breakFrom &&
                    minute<window.breakTo && minute+30>window.breakFrom
                val occupied=entries.any{s->
                    val slotMillis=Jalali.millis(day,slotStart)
                    slotMillis<s.startMillis+s.minutes*60000L &&
                        slotMillis+30*60000L>s.startMillis
                }
                Row(Modifier.fillMaxWidth().offset(y=((minute-start)*ppm).dp)
                    .height((30*ppm).dp),
                    verticalAlignment=Alignment.CenterVertically){
                    Text(WorkingHours.clock(minute).faDigits(),fontSize=11.sp,
                        color=Slate,modifier=Modifier.width(55.dp))
                    Surface(onClick={onAdd(day,slotStart)},enabled=!withinBreak && !occupied,
                        modifier=Modifier.weight(1f).height((30*ppm-3).dp),
                        shape=RoundedCornerShape(6.dp),
                        color=if(withinBreak) Color(0xFFFFF3E4) else Color(0xFFF9FCFA)){
                        Row(Modifier.fillMaxSize().padding(horizontal=9.dp),
                            verticalAlignment=Alignment.CenterVertically){
                            if(withinBreak)Text("استراحت",fontSize=10.sp,color=Color(0xFF9B6A2B))
                            else if(!occupied)Text("＋",color=Color(0xFF96B6A4),fontSize=14.sp)
                        }
                    }
                }
            }
            entries.forEach{s->
                val time=Jalali.time(s.startMillis).toLocalTime()
                val minute=time.hour*60+time.minute
                if(minute>=start && minute<end){
                    Box(Modifier.fillMaxWidth()
                        .offset(y=((minute-start)*ppm).dp)
                        .padding(start=56.dp)
                        .height((s.minutes*ppm).coerceAtLeast(47f).dp)
                    ){TimelineEvent(s,names[s.clientId]?:"مراجع"){onEdit(s)}}
                }
            }
        }
    }
}
