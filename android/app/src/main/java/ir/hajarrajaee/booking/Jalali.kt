package ir.hajarrajaee.booking

import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.*

data class JalaliDay(val year:Int,val month:Int,val day:Int)
object Jalali {
    val zone:ZoneId=ZoneId.of("Asia/Tehran")
    val months=listOf("فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور","مهر","آبان","آذر","دی","بهمن","اسفند")
    val weekdays=listOf("شنبه","یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه")
    fun today():LocalDate=LocalDate.now(zone)
    fun jalali(date:LocalDate):JalaliDay {
        val c=Calendar.getInstance(TimeZone.getTimeZone("UTC"),ULocale("fa_IR@calendar=persian"))
        c.timeInMillis=date.atTime(12,0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return JalaliDay(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH))
    }
    fun gregorian(j:JalaliDay):LocalDate {
        require(j.month in 1..12 && j.day in 1..31)
        val c=Calendar.getInstance(TimeZone.getTimeZone("UTC"),ULocale("fa_IR@calendar=persian"))
        c.clear();c.set(Calendar.YEAR,j.year);c.set(Calendar.MONTH,j.month-1)
        c.set(Calendar.DAY_OF_MONTH,j.day);c.set(Calendar.HOUR_OF_DAY,12)
        val d=Instant.ofEpochMilli(c.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate()
        require(jalali(d)==j) {"تاریخ شمسی نامعتبر"}
        return d
    }
    fun daysInMonth(y:Int,m:Int):Int {
        val start=gregorian(JalaliDay(y,m,1))
        val next=if(m==12) gregorian(JalaliDay(y+1,1,1)) else gregorian(JalaliDay(y,m+1,1))
        return (next.toEpochDay()-start.toEpochDay()).toInt()
    }
    fun weekday(date:LocalDate):Int=(date.dayOfWeek.value+1)%7
    fun monthStart(d:LocalDate):LocalDate {
        val j=jalali(d);return gregorian(JalaliDay(j.year,j.month,1))
    }
    fun addMonth(date:LocalDate,months:Int):LocalDate {
        val j=jalali(date)
        val index=j.year*12+j.month-1+months
        val year=Math.floorDiv(index,12);val month=Math.floorMod(index,12)+1
        return gregorian(JalaliDay(year,month,minOf(j.day,daysInMonth(year,month))))
    }
    fun label(d:LocalDate):String {
        val j=jalali(d)
        return (j.day.toString()+" "+months[j.month-1]+" "+j.year).faDigits()
    }
    fun monthLabel(d:LocalDate):String {
        val j=jalali(d);return (months[j.month-1]+" "+j.year).faDigits()
    }
    fun time(millis:Long):LocalDateTime=Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime()
    fun millis(d:LocalDate,t:LocalTime):Long=d.atTime(t).atZone(zone).toInstant().toEpochMilli()
    fun format(millis:Long):String {
        val d=time(millis);return label(d.toLocalDate())+" | "+d.toLocalTime().toString().faDigits()
    }
}

enum class Repeat(val label:String) {
    NONE("بدون تکرار"),DAILY("روزانه"),WEEKLY("هفتگی"),BIWEEKLY("هر دو هفته"),
    MONTHLY("ماهیانه بر اساس روز شمسی"),MONTH_WEEK("ماهانه بر اساس هفته و روز"),CUSTOM("تاریخ‌های دلخواه")
}
fun recurrenceDates(first:LocalDate,mode:Repeat,count:Int,selected:Set<LocalDate>):List<LocalDate> {
    val n=count.coerceIn(1,104)
    if(mode==Repeat.NONE)return listOf(first)
    if(mode==Repeat.CUSTOM)return (selected+first).filter { !it.isBefore(first) }.sorted().take(n)
    val firstJ=Jalali.jalali(first)
    return (0 until n).map { i ->
        when(mode) {
            Repeat.DAILY->first.plusDays(i.toLong())
            Repeat.WEEKLY->first.plusWeeks(i.toLong())
            Repeat.BIWEEKLY->first.plusWeeks(i.toLong()*2)
            Repeat.MONTHLY->{
                val newMonth=Jalali.addMonth(Jalali.gregorian(JalaliDay(firstJ.year,firstJ.month,1)),i)
                val j=Jalali.jalali(newMonth)
                Jalali.gregorian(JalaliDay(j.year,j.month,minOf(firstJ.day,Jalali.daysInMonth(j.year,j.month))))
            }
            Repeat.MONTH_WEEK->{
                val newMonth=Jalali.addMonth(Jalali.monthStart(first),i)
                val start=Jalali.monthStart(newMonth)
                val nth=(firstJ.day-1)/7
                val offset=(Jalali.weekday(first)-Jalali.weekday(start)+7)%7
                var proposed=start.plusDays((offset+nth*7).toLong())
                if(Jalali.jalali(proposed).month!=Jalali.jalali(start).month)proposed=proposed.minusWeeks(1)
                proposed
            }
            else->first
        }
    }.distinct()
}
