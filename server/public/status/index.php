<?php
declare(strict_types=1);
header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('Referrer-Policy: no-referrer');
header('X-Frame-Options: DENY');
?><!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex,nofollow">
<title>پیگیری نوبت | وقت‌یار</title><style>
body{background:#f4faf5;font-family:Vazirmatn,Tahoma,sans-serif;color:#09291d;margin:0;padding:20px}
main{max-width:440px;margin:40px auto;background:white;padding:26px;border-radius:24px;text-align:center;box-shadow:0 10px 30px #194d3019}
h1{font-size:23px}p{line-height:2}button{background:#23a765;color:white;border:0;border-radius:13px;padding:14px 22px;font:inherit}
</style></head><body><main><h1>وضعیت درخواست نوبت 🌿</h1><p id="state">در حال دریافت وضعیت...</p><p id="date"></p>
<button id="refresh">بررسی مجدد</button><p><small>تأیید نهایی فقط توسط مدیر انجام می‌شود.</small></p></main>
<script>
const token=new URLSearchParams(location.search).get("token")||"";
const names={pending:"در انتظار تأیید مدیر",approved:"✅ نوبت تأیید شده است",rejected:"درخواست رد شد",expired:"مهلت تأیید به پایان رسیده است",cancelled:"درخواست لغو شده است"};
async function load(){
 const box=document.getElementById("state");
 if(!/^[0-9a-f]{64}$/.test(token)){box.textContent="لینک نامعتبر است";return}
 try{
  const response=await fetch("/api/v1/booking-status/"+token,{cache:"no-store"});
  const data=await response.json();
  if(!response.ok)throw new Error(data.error||"خطا در پیگیری");
  box.textContent=names[data.status]||"وضعیت نامشخص";
  document.getElementById("date").textContent=data.starts_at?"ساعت انتخابی به وقت ایران: "+data.starts_at:"";
 }catch(e){box.textContent=e.message}
}
document.getElementById("refresh").onclick=load;load();
</script></body></html>
