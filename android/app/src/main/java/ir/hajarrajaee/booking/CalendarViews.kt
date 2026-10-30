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
                            Text(j.day.toString(),fontSize=13.sp,
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
    onEdit:(LocalSession)->Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    val daySessions=sessions.filter {
        Jalali.time(it.startMillis).toLocalDate()==selected && it.status=="approved"
    }.sortedBy{it.startMillis}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(15.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("تقویم شمسی",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Forest)
        SingleChoiceSegmentedButtonRow(modifier=Modifier.fillMaxWidth()){
            listOf("ماهانه","هفتگی","روزانه").forEachIndexed {i,label->
                SegmentedButton(selected=mode==i,onClick={mode=i},
                    shape=SegmentedButtonDefaults.itemShape(index=i,count=3)){
                    Text(label)
                }
            }
        }
        Card(shape=RoundedCornerShape(22.dp),
            colors=CardDefaults.cardColors(containerColor=Color.White)){
            Column(Modifier.padding(13.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
                when(mode) {
                    0->JalaliMonthPicker(selected,onSelect,sessions)
                    1->{
                        val start=selected.minusDays(Jalali.weekday(selected).toLong())
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                            IconButton(onClick={onSelect(selected.minusWeeks(1))}){Text("‹",fontSize=26.sp)}
                            Text("هفته "+Jalali.label(start),color=Forest,
                                modifier=Modifier.align(Alignment.CenterVertically))
                            IconButton(onClick={onSelect(selected.plusWeeks(1))}){Text("›",fontSize=26.sp)}
                        }
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(2.dp)){
                            (0..6).forEach { offset ->
                                val d=start.plusDays(offset.toLong())
                                val total=sessions.count{
                                    it.status=="approved" && Jalali.time(it.startMillis).toLocalDate()==d
                                }
                                Surface(onClick={onSelect(d)},modifier=Modifier.weight(1f).height(73.dp),
                                    shape=RoundedCornerShape(10.dp),
                                    color=if(d==selected)Forest else Pale){
                                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                                        verticalArrangement=Arrangement.Center){
                                        Text(Jalali.weekdays[offset].take(3),fontSize=10.sp,
                                            color=if(d==selected)Color.White else Slate)
                                        Text(Jalali.jalali(d).day.toString(),fontSize=17.sp,
                                            color=if(d==selected)Color.White else Forest)
                                        if(total>0)Text(total.toString()+" ●",fontSize=10.sp,
                                            color=if(d==selected)Color.White else Green)
                                    }
                                }
                            }
                        }
                    }
                    else->{
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                            verticalAlignment=Alignment.CenterVertically){
                            IconButton(onClick={onSelect(selected.minusDays(1))}){Text("‹",fontSize=26.sp)}
                            Text(Jalali.label(selected),fontWeight=FontWeight.Bold,color=Forest)
                            IconButton(onClick={onSelect(selected.plusDays(1))}){Text("›",fontSize=26.sp)}
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
            Text(Jalali.label(selected),fontWeight=FontWeight.Bold,fontSize=18.sp,color=Forest)
            Text(daySessions.size.toString()+" نوبت",color=Slate)
        }
        if(mode==2) {
            // Minute-accurate events are shown under their real start hour, never snapped.
            (9..21).forEach { hour ->
                val items=daySessions.filter{Jalali.time(it.startMillis).hour==hour}
                Row(Modifier.fillMaxWidth().padding(vertical=3.dp),
                    horizontalArrangement=Arrangement.spacedBy(9.dp)){
                    Text(hour.toString().padStart(2,'0')+":00",
                        fontSize=12.sp,color=Slate,modifier=Modifier.width(52.dp).padding(top=12.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        if(items.isEmpty())
                            HorizontalDivider(color=Color(0xFFD6E7DA),modifier=Modifier.padding(top=16.dp))
                        items.forEach { s ->
                            val name=clients.firstOrNull{it.id==s.clientId}?.name?:"مراجع"
                            SessionCard(s,name){onEdit(s)}
                        }
                    }
                }
            }
        } else if(daySessions.isEmpty()){
            Card(shape=RoundedCornerShape(15.dp)){
                Text("نوبتی برای این روز ثبت نشده است.",
                    modifier=Modifier.fillMaxWidth().padding(18.dp),color=Slate)
            }
        } else daySessions.forEach {s->
            val name=clients.firstOrNull{it.id==s.clientId}?.name?:"مراجع"
            SessionCard(s,name){onEdit(s)}
        }
        Text("با لمس هر جلسه می‌توان زمان آن را تغییر داد یا نوبت را حذف کرد.",
            fontSize=12.sp,color=Slate)
    }
}
