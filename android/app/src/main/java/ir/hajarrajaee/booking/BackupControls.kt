package ir.hajarrajaee.booking

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun BackupControls(context:Context,onRestored:()->Unit) {
    var password by remember {mutableStateOf("")}
    var message by remember {mutableStateOf("")}
    var confirm by remember {mutableStateOf(false)}
    var incoming by remember {mutableStateOf<ByteArray?>(null)}
    val scope=rememberCoroutineScope()
    val create=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
        if(uri!=null)scope.launch {
            try {
                val bytes=withContext(Dispatchers.IO){BookingStore(context).use { SecureBackup.export(it,password) }}
                withContext(Dispatchers.IO){context.contentResolver.openOutputStream(uri)?.use{it.write(bytes)}
                    ?: error("فایل قابل نوشتن نیست")}
                message="بکاپ رمزگذاری‌شده ذخیره شد. رمز بکاپ را در محل امن نگه دارید."
            }catch(e:Exception){message="خطا: "+(e.message?:"ذخیره نشد")}
        }
    }
    val open=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->
        if(uri!=null)scope.launch {
            try{
                incoming=withContext(Dispatchers.IO){
                    context.contentResolver.openInputStream(uri)?.use{
                        val data=it.readBytes()
                        require(data.size<=15_000_000){"حجم فایل بیش از حد مجاز است"}
                        data
                    }?:error("فایل خوانده نشد")
                }
                confirm=true
            }catch(e:Exception){message="خطا: "+e.message}
        }
    }
    Card(shape=RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(15.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Text("بکاپ رمزگذاری‌شده و بازیابی",style=MaterialTheme.typography.titleMedium)
            Text("فایل بکاپ با AES-256-GCM و رمز مستقل محافظت می‌شود. بازیابی، تمام اطلاعات فعلی را جایگزین خواهد کرد.",
                color=Slate)
            OutlinedTextField(password,{password=it},modifier=Modifier.fillMaxWidth(),
                label={Text("رمز اختصاصی بکاپ (حداقل ۱۲ کاراکتر)")},
                visualTransformation=PasswordVisualTransformation(),singleLine=true)
            OutlinedButton(onClick={create.launch("VaghYar-Backup.vybk")},enabled=password.length>=12){
                Text("خروجی بکاپ رمزگذاری‌شده")
            }
            OutlinedButton(onClick={open.launch(arrayOf("*/*"))},enabled=password.length>=12) {
                Text("انتخاب فایل برای بازیابی")
            }
            if(message.isNotBlank())Text(message,color=Slate)
        }
    }
    if(confirm && incoming!=null)AlertDialog(
        onDismissRequest={confirm=false;incoming=null},
        title={Text("تأیید جایگزینی کامل اطلاعات")},
        text={Text("با بازیابی، تمامی پرونده‌ها و نوبت‌های فعلی جایگزین می‌شوند. قبل از ادامه بکاپ فعلی بگیرید.")},
        confirmButton={Button(onClick={
            val bytes=incoming
            confirm=false;incoming=null
            if(bytes!=null)scope.launch{
                try {
                    val count=withContext(Dispatchers.IO) {
                        BookingStore(context).use{SecureBackup.restore(it,bytes,password)}
                    }
                    ReminderScheduler.refresh(context)
                    onRestored()
                    message="بازیابی با موفقیت انجام شد: "+count+" نوبت"
                }catch(e:Exception){message="بازیابی ناموفق: "+(e.message?:"رمز یا فایل اشتباه")}
            }
        }){Text("بازیابی و جایگزینی")}},
        dismissButton={TextButton(onClick={confirm=false;incoming=null}){Text("انصراف")}}
    )
}
