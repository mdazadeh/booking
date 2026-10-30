<?php
declare(strict_types=1);
header('Content-Type: text/html; charset=utf-8');
header('Cache-Control: no-store');
header('Referrer-Policy: no-referrer');
header('X-Frame-Options: DENY');
?><!doctype html><html lang="fa" dir="rtl">
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<meta name="robots" content="noindex,nofollow"><meta name="theme-color" content="#09291d">
<title>رزرو اختصاصی | وقت‌یار</title>
<style>
:root{font-family:Vazirmatn,Tahoma,sans-serif;color:#09291d;background:#f4faf5}
*{box-sizing:border-box}body{margin:0;background:linear-gradient(135deg,#d2ebd9,#f4faf5 50%)}
header{background:#09291d;color:white;padding:28px 24px 45px;border-radius:0 0 30px 30px}
main{max-width:520px;margin:-20px auto 0;padding:0 16px 30px;position:relative}
h1{font-size:25px}h2{font-size:17px;margin:0 0 12px}
section{background:white;padding:20px;border-radius:24px;margin-bottom:14px;box-shadow:0 12px 36px #0c402018}
p{line-height:2}.hint{color:#577364;font-size:13px}
#days{display:flex;gap:8px;overflow-x:auto;padding-bottom:8px}
.day{min-width:112px;min-height:67px;border:1px solid #dce9de;background:#f5faf6;border-radius:16px;color:#123629}
.day.active{background:#09291d;color:white}
#slots{display:grid;grid-template-columns:repeat(3,1fr);gap:9px}
.slot{padding:13px 5px;border:1px solid #d9eadc;border-radius:13px;background:#f5faf6;color:#115735}
.slot.active{background:#23a765;color:white}
button{font-family:inherit;cursor:pointer;touch-action:manipulation}button:disabled{opacity:.45}
#send{width:100%;min-height:52px;background:#23a765;border:0;border-radius:14px;color:white;font-size:16px;font-weight:bold}
.notice{padding:12px;background:#ecf8ed;border-radius:12px;font-size:13px}
#result{font-size:14px;word-break:break-word}
</style></head><body>
<header><div style="max-width:480px;margin:auto"><small>VAGHYAR • PRIVATE BOOKING</small>
<h1>درخواست نوبت 🌿</h1><p id="client" style="color:#cae6d4">در حال بررسی دعوت‌نامه...</p></div></header>
<main>
<section><h2>۱. انتخاب تاریخ شمسی</h2><p class="hint">روزهای آینده به وقت ایران</p><div id="days"></div></section>
<section><h2>۲. انتخاب ساعت آزاد</h2><div id="slots"></div><p class="hint" id="message"></p></section>
<section><p class="notice">رزرو فقط پس از تأیید مدیر قطعی می‌شود. لینک پیگیری پس از ثبت درخواست نمایش داده خواهد شد.</p>
<button id="send" disabled>ارسال درخواست نوبت</button><p id="result" role="status"></p></section>
</main><script>
"use strict";
const token=location.pathname.split("/").filter(Boolean).pop()||"";
const base="/api/v1/invitations/"+encodeURIComponent(token);
const days=document.getElementById("days"), slots=document.getElementById("slots");
const result=document.getElementById("result"),message=document.getElementById("message");
const submit=document.getElementById("send");
let day="",time="",valid=false;
async function getJson(url,options={}) {
 const response=await fetch(url,{...options,cache:"no-store",credentials:"omit"});
 const obj=await response.json();
 if(!response.ok)throw new Error(obj.error||"خطای سرور");
 return obj;
}
function todayIran(){
 const parts=new Intl.DateTimeFormat("en-US",{timeZone:"Asia/Tehran",year:"numeric",month:"2-digit",day:"2-digit"}).formatToParts(new Date());
 const get=t=>parts.find(x=>x.type===t).value;
 return get("year")+"-"+get("month")+"-"+get("day");
}
async function chooseDay(date,button){
 day=date;time="";submit.disabled=true;slots.replaceChildren();message.textContent="در حال دریافت ساعت‌ها...";
 days.querySelectorAll("button").forEach(b=>b.classList.remove("active"));button.classList.add("active");
 try{
  const data=await getJson(base+"/availability?day="+encodeURIComponent(date));
  for(const slot of data.slots||[]){
   const b=document.createElement("button");b.className="slot";b.textContent=slot;
   b.onclick=()=>{time=slot;slots.querySelectorAll("button").forEach(e=>e.classList.remove("active"));b.classList.add("active");submit.disabled=false};
   slots.append(b);
  }
  message.textContent=slots.children.length?"ساعت موردنظر را انتخاب کنید":"در این روز ظرفیت آزادی وجود ندارد";
 }catch(e){message.textContent=e.message}
}
function makeDays(){
 const baseDate=new Date(todayIran()+"T12:00:00Z");
 const persian=new Intl.DateTimeFormat("fa-IR-u-ca-persian",{weekday:"short",month:"short",day:"numeric",timeZone:"UTC"});
 for(let offset=0;offset<30;offset++){
  const date=new Date(baseDate.getTime()+offset*86400000);
  const iso=date.toISOString().slice(0,10);
  const b=document.createElement("button");
  b.className="day";b.textContent=persian.format(date);b.onclick=()=>chooseDay(iso,b);
  days.append(b);
 }
}
submit.onclick=async()=>{
 if(!day||!time||!valid)return;submit.disabled=true;result.textContent="در حال ثبت درخواست...";
 try{
  const data=await getJson(base+"/requests",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({day,time})});
  const link=document.createElement("a");link.href=location.origin+"/status/?token="+encodeURIComponent(data.tracking_token);
  link.textContent="مشاهده و پیگیری درخواست";
  result.textContent="درخواست ثبت شد و در انتظار تأیید مدیر است.";
  result.append(document.createElement("br"),link);
  slots.replaceChildren();valid=false;
 }catch(e){result.textContent=e.message;submit.disabled=false}
};
(async()=>{
 if(!/^[0-9a-f]{64}$/.test(token)){result.textContent="لینک معتبر نیست";return}
 try{
  const data=await getJson(base);document.getElementById("client").textContent="دعوت‌نامه اختصاصی: "+data.name;
  valid=true;makeDays();
 }catch(e){result.textContent=e.message;document.getElementById("client").textContent="لینک در دسترس نیست"}
})();
</script></body></html>
