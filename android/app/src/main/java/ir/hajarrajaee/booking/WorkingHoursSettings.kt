package ir.hajarrajaee.booking

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun WorkingHoursSettings(context:Context,onSaved:()->Unit){
    var selected by remember{mutableIntStateOf(0)}
    var revision by remember{mutableIntStateOf(0)}
    val settings=remember(selected,revision){WorkingHours.get(context,selected)}
    var enabled by remember(selected,revision){mutableStateOf(settings.enabled)}
    var start by remember(selected,revision){mutableStateOf(WorkingHours.clock(settings.open))}
    var end by remember(selected,revision){mutableStateOf(WorkingHours.clock(settings.close))}
    var message by remember{mutableStateOf("")}
    Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=androidx.compose.ui.graphics.Color.White)){
        Column(Modifier.fillMaxWidth().padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("🌈 ساعات کاری هفتگی",style=MaterialTheme.typography.titleMedium)
            Text("روزهای فعال و ساعت شروع و پایان را تعیین کنید. انتشار این برنامه در سایت، یک مرحله جداگانه است.",
                style=MaterialTheme.typography.bodySmall,color=Slate)
            Row(Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement=Arrangement.spacedBy(5.dp)){
                WorkingHours.labels.forEachIndexed{i,label->
                    FilterChip(selected=selected==i,onClick={selected=i;message=""},label={Text(label)})
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("روز کاری",style=MaterialTheme.typography.bodyLarge)
                Switch(checked=enabled,onCheckedChange={enabled=it})
            }
            if(enabled){
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    ClockField("شروع",start,{start=it},Modifier.weight(1f))
                    ClockField("پایان",end,{end=it},Modifier.weight(1f))
                }
            }
            Button(onClick={
                try{
                    val w=WorkWindow(enabled,
                        WorkingHours.parseClock(start)?:error("ساعت شروع نامعتبر است."),
                        WorkingHours.parseClock(end)?:error("ساعت پایان نامعتبر است."),
                        0,0)
                    WorkingHours.save(context,selected,w)
                    revision++;message="برنامه "+WorkingHours.labels[selected]+" ذخیره شد."
                    onSaved()
                }catch(e:Exception){message=e.message?:"تنظیمات نامعتبر است."}
            },modifier=Modifier.fillMaxWidth()){Text("ذخیره برنامه کاری")}
            if(message.isNotBlank())Text(message,color=Slate)
        }
    }
}

@Composable
private fun ClockField(title:String,value:String,onValue:(String)->Unit,modifier:Modifier){
    OutlinedTextField(value.faDigits(),{onValue(it.asciiDigits().take(5))},
        modifier=modifier,label={Text(title)},
        singleLine=true,
        keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),
        textStyle=androidx.compose.ui.text.TextStyle(
            textDirection=androidx.compose.ui.text.style.TextDirection.Ltr,
            fontFamily=VaghFontFamily))
}
