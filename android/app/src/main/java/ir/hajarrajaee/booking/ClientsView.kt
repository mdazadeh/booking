package ir.hajarrajaee.booking

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId

@Composable
fun ClientsPanel(
    store:BookingStore,
    clients:List<LocalClient>,
    sessions:List<LocalSession>,
    onChange:()->Unit
){
    var search by remember{mutableStateOf("")}
    var selectedId by remember{mutableStateOf<Long?>(null)}
    var editId by remember{mutableStateOf<Long?>(null)}
    var showForm by remember{mutableStateOf(false)}
    var name by remember{mutableStateOf("")}
    var phone by remember{mutableStateOf("")}
    var summary by remember{mutableStateOf("")}
    var formError by remember{mutableStateOf("")}
    var noteText by remember{mutableStateOf("")}
    var noteSession by remember{mutableStateOf<Long?>(null)}
    var noteRevision by remember{mutableIntStateOf(0)}
    var deleteNote by remember{mutableStateOf<LocalNote?>(null)}
    fun openEditor(client:LocalClient?) {
        editId=client?.id;name=client?.name?:"";phone=client?.phone?:""
        summary=client?.history?:"";formError="";showForm=true
    }
    val selected=clients.firstOrNull{it.id==selectedId}
    Column(Modifier.fillMaxSize().padding(15.dp)){
        if(selected==null){
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                Text("پرونده مراجعان",fontSize=22.sp,fontWeight=FontWeight.Bold,color=Forest)
                Button(onClick={openEditor(null)}){Text("+ مراجع جدید")}
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),
                label={Text("جست‌وجوی نام یا شماره تماس")},singleLine=true)
            Spacer(Modifier.height(12.dp))
            val found=clients.filter {it.name.contains(search,true)||it.phone.contains(search.asciiDigits(),true)}
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){
                items(found,key={it.id}){c->
                    Card(onClick={selectedId=c.id},shape=RoundedCornerShape(16.dp),
                        modifier=Modifier.fillMaxWidth()){
                        Column(Modifier.padding(16.dp)){
                            Text(c.name,fontWeight=FontWeight.Bold,fontSize=17.sp,color=Forest)
                            if(c.phone.isNotBlank())Text(c.phone.faDigits(),fontSize=13.sp,color=Slate)
                            Text(sessions.count{it.clientId==c.id}.toString().faDigits()+" جلسه ثبت‌شده",
                                fontSize=12.sp,color=Green)
                        }
                    }
                }
                if(found.isEmpty())item{Text("مراجعی ثبت نشده است.",color=Slate)}
            }
        }else{
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){
                TextButton(onClick={selectedId=null;noteSession=null}){Text("بازگشت")}
                Text("پرونده مراجع",fontWeight=FontWeight.Bold,fontSize=19.sp,color=Forest)
                TextButton(onClick={openEditor(selected)}){Text("ویرایش")}
            }
            val clientSessions=sessions.filter{it.clientId==selected.id}.sortedByDescending{it.startMillis}
            val notes=remember(selected.id,noteRevision){store.notes(selected.id)}
            LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)){
                item{
                    Card(shape=RoundedCornerShape(21.dp),
                        colors=CardDefaults.cardColors(containerColor=Forest)){
                        Column(Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text(selected.name,fontSize=22.sp,fontWeight=FontWeight.Bold,
                                color=androidx.compose.ui.graphics.Color.White)
                            Text(if(selected.phone.isBlank())"شماره تماس ثبت نشده" else selected.phone.faDigits(),
                                color=androidx.compose.ui.graphics.Color(0xFFD0EBD7))
                            Text("شرح حال اولیه",fontWeight=FontWeight.Bold,color=androidx.compose.ui.graphics.Color.White)
                            Text(if(selected.history.isBlank())"شرح حالی ثبت نشده است." else selected.history,
                                color=androidx.compose.ui.graphics.Color(0xFFDEEFE2),lineHeight=24.sp)
                        }
                    }
                }
                item{
                    Card(shape=RoundedCornerShape(18.dp)){
                        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
                            Text("یادداشت جدید / گزارش جلسه",fontWeight=FontWeight.Bold)
                            Text("یادداشت را می‌توان به یک جلسه مشخص مرتبط کرد.",
                                color=Slate,fontSize=12.sp)
                            Row(Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement=Arrangement.spacedBy(6.dp)){
                                FilterChip(selected=noteSession==null,onClick={noteSession=null},
                                    label={Text("یادداشت عمومی")})
                                clientSessions.take(20).forEach {s->
                                    FilterChip(selected=noteSession==s.id,
                                        onClick={noteSession=s.id},
                                        label={Text(Jalali.format(s.startMillis),fontSize=11.sp)})
                                }
                            }
                            OutlinedTextField(noteText,{noteText=it},modifier=Modifier.fillMaxWidth(),
                                label={Text("شرح جلسه، وضعیت و توضیحات")},minLines=3,maxLines=8)
                            Button(onClick={
                                try{
                                    store.addNote(selected.id,noteSession,noteText)
                                    noteText="";noteSession=null;noteRevision++
                                }catch(e:Exception){formError=e.message?:"خطا در ذخیره"}
                            },enabled=noteText.isNotBlank(),modifier=Modifier.fillMaxWidth()){
                                Text("ذخیره یادداشت")
                            }
                            if(formError.isNotBlank())Text(formError,color=androidx.compose.ui.graphics.Color(0xFFAD4335))
                        }
                    }
                }
                item{Text("تاریخچه یادداشت‌ها",fontSize=18.sp,fontWeight=FontWeight.Bold)}
                items(notes,key={it.id}){note->
                    Card(shape=RoundedCornerShape(16.dp)){
                        Column(Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement=Arrangement.spacedBy(5.dp)){
                            Text(note.body,color=Forest,lineHeight=22.sp)
                            Text(Jalali.format(note.createdAt)+
                                (if(note.sessionId!=null)" • پیوست جلسه" else " • یادداشت عمومی"),
                                color=Slate,fontSize=12.sp)
                            TextButton(onClick={deleteNote=note}){Text("حذف یادداشت")}
                        }
                    }
                }
                item{Text("سوابق نوبت‌ها",fontSize=18.sp,fontWeight=FontWeight.Bold)}
                items(clientSessions,key={it.id}){s->SessionCard(s,selected.name,null)}
                if(notes.isEmpty() && clientSessions.isEmpty())item{
                    Text("هنوز جلسه یا یادداشتی برای این مراجع وجود ندارد.",color=Slate)
                }
            }
        }
    }

    if(showForm){
        AlertDialog(onDismissRequest={showForm=false},
            title={Text(if(editId==null)"افزودن مراجع" else "ویرایش پرونده")},
            text={
                Column(Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement=Arrangement.spacedBy(9.dp)){
                    OutlinedTextField(name,{name=it},label={Text("نام و نام خانوادگی *")},
                        modifier=Modifier.fillMaxWidth(),singleLine=true)
                    PhoneNumberField(phone,{phone=it})
                    OutlinedTextField(summary,{summary=it},label={Text("شرح حال و اطلاعات اولیه")},
                        modifier=Modifier.fillMaxWidth(),minLines=4,maxLines=7)
                    if(formError.isNotBlank())Text(formError,color=androidx.compose.ui.graphics.Color(0xFFAD4335))
                }
            },
            confirmButton={
                Button(onClick={
                    try{
                        selectedId=store.saveClient(editId,name,canonicalPhone(phone),summary)
                        showForm=false;formError="";onChange()
                    }catch(e:Exception){formError=e.message?:"خطا در ذخیره"}
                },enabled=name.isNotBlank()){Text("ذخیره پرونده")}
            },
            dismissButton={TextButton(onClick={showForm=false}){Text("انصراف")}}
        )
    }
    deleteNote?.let { target ->
        AlertDialog(onDismissRequest={deleteNote=null},
            title={Text("حذف یادداشت؟")},text={Text("این یادداشت از پرونده حذف می‌شود.")},
            confirmButton={Button(onClick={
                store.removeNote(target.id);noteRevision++;deleteNote=null
            }){Text("حذف")}},
            dismissButton={TextButton(onClick={deleteNote=null}){Text("انصراف")}}
        )
    }
}
