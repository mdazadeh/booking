package ir.hajarrajaee.booking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.icu.util.PersianCalendar
import android.icu.util.Calendar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneId

private val green = Color(0xFF23A765)
private val dark = Color(0xFF09291D)
private val bg = Color(0xFFF4FAF5)
private val muted = Color(0xFF587364)
private val tehran = ZoneId.of("Asia/Tehran")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = green, background = bg)) {
                    BookingScreen(this)
                }
            }
        }
    }
}

private fun jalali(date: String): String = try {
    val d = LocalDate.parse(date.take(10))
    val p = PersianCalendar()
    p.timeInMillis = d.atStartOfDay(tehran).toInstant().toEpochMilli()
    p.get(Calendar.YEAR).toString() + "/" +
        (p.get(Calendar.MONTH)+1).toString().padStart(2,'0') + "/" +
        p.get(Calendar.DAY_OF_MONTH).toString().padStart(2,'0')
} catch (_: Exception) { date }

private fun pretty(value: String): String =
    if (value.length >= 16) jalali(value) + " | " + value.substring(11,16) else value

private suspend fun request(method: String, endpoint: String, token: String = "", body: JSONObject? = null): JSONObject =
    withContext(Dispatchers.IO) {
        val conn = (URL(BuildConfig.API_BASE_URL + endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer " + token)
            if (body != null) {
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                doOutput = true
                outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
        }
        try {
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val result = JSONObject(stream?.bufferedReader()?.use { it.readText() } ?: "{}")
            if (status !in 200..299) throw IllegalStateException(result.optString("error", "خطای سرور: " + status))
            result
        } finally { conn.disconnect() }
    }

data class Entry(val id: Long, val name: String, val time: String, val status: String)
@Composable
fun BookingScreen(context: Context) {
    val preferences = remember { context.getSharedPreferences("booking_auth", Context.MODE_PRIVATE) }
    var token by remember { mutableStateOf(preferences.getString("token", "") ?: "") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var section by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var requests by remember { mutableStateOf(listOf<Entry>()) }
    var appointments by remember { mutableStateOf(listOf<Entry>()) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var inviteUrl by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualDate by remember { mutableStateOf(LocalDate.now(tehran).toString()) }
    var manualTime by remember { mutableStateOf("10:00") }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            val result = request("GET", "/admin/overview", token)
            fun parse(a: JSONArray?): List<Entry> {
                if (a == null) return emptyList()
                return (0 until a.length()).map {
                    val o = a.getJSONObject(it)
                    Entry(o.optLong("id"), o.optString("name"), o.optString("starts_at"), o.optString("status"))
                }
            }
            requests = parse(result.optJSONArray("requests"))
            appointments = parse(result.optJSONArray("appointments"))
            error = ""
        } catch (e: Exception) { error = e.message ?: "اتصال برقرار نشد" }
    }
    LaunchedEffect(token) { if (token.isNotEmpty()) refresh() }
    Column(Modifier.fillMaxSize().background(bg)) {
        Row(Modifier.fillMaxWidth().background(dark).padding(22.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("وقت‌یار", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 27.sp)
                Text(jalali(LocalDate.now(tehran).toString()) + " • ساعت ایران",
                    color = Color(0xFFB7DEC8))
            }
            if (token.isNotEmpty()) TextButton(onClick = {
                preferences.edit().remove("token").apply()
                token = ""; requests = emptyList(); appointments = emptyList()
            }) { Text("خروج", color = Color.White) }
        }
        if (token.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("ورود مدیر", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(username, { username = it }, label = { Text("نام کاربری") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(password, { password = it }, label = { Text("رمز عبور") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            scope.launch {
                                busy = true
                                try {
                                    val auth = request("POST", "/auth/login", body = JSONObject()
                                        .put("username", username).put("password", password))
                                    token = auth.getString("token")
                                    preferences.edit().putString("token", token).apply()
                                    password = ""; error = ""
                                } catch(e: Exception) { error = e.message ?: "ورود ناموفق" }
                                busy = false
                            }
                        }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("ورود") }
                    }
                }
            }
        } else {
            TabRow(selectedTabIndex = section) {
                listOf("درخواست‌ها", "لینک خصوصی", "نوبت‌ها").forEachIndexed { i, label ->
                    Tab(selected = section == i, onClick = { section = i }, text = { Text(label) })
                }
            }
            when (section) {
                0 -> Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("در انتظار بررسی", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { scope.launch { refresh() } }) { Text("بروزرسانی") }
                    }
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(requests) { item ->
                            Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text(pretty(item.time), color = muted)
                                    Text(when(item.status) {
                                        "pending" -> "در انتظار تأیید"
                                        "approved" -> "تأیید شده"
                                        "rejected" -> "رد شده"
                                        "expired" -> "منقضی شده"
                                        else -> item.status
                                    }, color = green)
                                    if (item.status == "pending") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(onClick = {
                                            scope.launch {
                                                try { request("POST", "/admin/requests/" + item.id + "/approve", token); refresh() }
                                                catch(e: Exception) { error = e.message ?: "خطا" }
                                            }
                                        }) { Text("تأیید") }
                                        OutlinedButton(onClick = {
                                            scope.launch {
                                                try { request("POST", "/admin/requests/" + item.id + "/reject", token); refresh() }
                                                catch(e: Exception) { error = e.message ?: "خطا" }
                                            }
                                        }) { Text("رد") }
                                    }
                                }
                            }
                        }
                        if(requests.isEmpty()) item { Text("درخواستی موجود نیست.", color = muted) }
                    }
                }
                1 -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
                    Card(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("دعوت‌نامه اختصاصی مشتری", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("مشتری نیازی به نصب اپلیکیشن ندارد.", color = muted)
                            OutlinedTextField(name, { name = it }, label = { Text("نام مراجع") },
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(phone, { phone = it }, label = { Text("شماره تماس (اختیاری)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        val result = request("POST", "/admin/invitations", token,
                                            JSONObject().put("name", name).put("phone", phone))
                                        inviteUrl = result.getString("url"); error = ""
                                    } catch(e: Exception) { error = e.message ?: "خطا" }
                                    busy = false
                                }
                            }, modifier = Modifier.fillMaxWidth(), enabled = !busy && name.isNotBlank()) {
                                Text("ایجاد لینک")
                            }
                            if (inviteUrl.isNotEmpty()) {
                                Text(inviteUrl, fontSize = 13.sp, color = green)
                                Button(onClick = {
                                    val share = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, inviteUrl)
                                    }
                                    context.startActivity(Intent.createChooser(share, "ارسال لینک"))
                                }, modifier = Modifier.fillMaxWidth()) { Text("اشتراک‌گذاری") }
                            }
                        }
                    }
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("نوبت‌های قطعی", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    appointments.forEach {
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Text(it.name, fontWeight = FontWeight.Bold)
                                Text(pretty(it.time), color = green)
                            }
                        }
                    }
                    Card(shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("ثبت نوبت دستی", fontWeight = FontWeight.Bold)
                            Text("ورود تاریخ در این نسخه آزمایشی میلادی است؛ نمایش نوبت شمسی است.",
                                fontSize = 13.sp, color = muted)
                            OutlinedTextField(manualName, { manualName = it }, label = { Text("نام مراجع") },
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(manualDate, { manualDate = it }, label = { Text("YYYY-MM-DD") },
                                modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(manualTime, { manualTime = it }, label = { Text("HH:mm") },
                                modifier = Modifier.fillMaxWidth())
                            Button(onClick = {
                                scope.launch {
                                    busy = true
                                    try {
                                        request("POST", "/admin/appointments", token, JSONObject()
                                            .put("name", manualName).put("day", manualDate).put("time", manualTime))
                                        manualName = ""; refresh()
                                    } catch(e: Exception) { error = e.message ?: "خطا" }
                                    busy = false
                                }
                            }, enabled = !busy && manualName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()) { Text("ثبت نوبت یک‌ساعته") }
                        }
                    }
                }
            }
        }
        if (error.isNotBlank()) Text(error, color = Color(0xFFA34018), modifier = Modifier.padding(12.dp))
    }
}
