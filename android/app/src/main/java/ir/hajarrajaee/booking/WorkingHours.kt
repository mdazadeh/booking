package ir.hajarrajaee.booking

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime

data class WorkWindow(val enabled:Boolean,val open:Int,val close:Int,val breakFrom:Int,val breakTo:Int){
    fun allows(start:LocalTime,minutes:Int):Boolean{
        if(!enabled)return false
        val s=start.hour*60+start.minute;val e=s+minutes
        return s>=open && e<=close && !(breakTo>breakFrom && s<breakTo && e>breakFrom)
    }
}
object WorkingHours{
    private const val NAME="vaghyar_working_hours_v1"
    val labels=listOf("شنبه","یکشنبه","دوشنبه","سه‌شنبه","چهارشنبه","پنجشنبه","جمعه")
    fun dayIndex(date:LocalDate):Int=(date.dayOfWeek.value+1)%7
    fun get(context:Context,index:Int):WorkWindow{
        require(index in 0..6)
        val p=context.getSharedPreferences(NAME,Context.MODE_PRIVATE)
        val k="d$index"
        return WorkWindow(p.getBoolean(k+"_on",index!=6),p.getInt(k+"_start",540),
            p.getInt(k+"_end",1140),p.getInt(k+"_breakstart",780),p.getInt(k+"_breakend",840))
    }
    fun save(context:Context,index:Int,w:WorkWindow){
        require(index in 0..6 && w.open in 0..1439 && w.close in 1..1440 && w.close>w.open)
        require(w.breakFrom in 0..1440 && w.breakTo in 0..1440)
        require(w.breakTo<=w.breakFrom || (w.breakFrom>=w.open && w.breakTo<=w.close))
        val k="d$index"
        context.getSharedPreferences(NAME,Context.MODE_PRIVATE).edit()
            .putBoolean(k+"_on",w.enabled).putInt(k+"_start",w.open).putInt(k+"_end",w.close)
            .putInt(k+"_breakstart",w.breakFrom).putInt(k+"_breakend",w.breakTo).apply()
    }
    fun forDate(context:Context,date:LocalDate)=get(context,dayIndex(date))
    fun validate(context:Context,date:LocalDate,start:LocalTime,minutes:Int):Boolean=
        forDate(context,date).allows(start,minutes)
    fun clock(minutes:Int):String="%02d:%02d".format(minutes/60,minutes%60)
    fun parseClock(input:String):Int?{
        val parts=input.asciiDigits().split(":")
        if(parts.size!=2)return null
        val h=parts[0].toIntOrNull()?:return null;val m=parts[1].toIntOrNull()?:return null
        if(h !in 0..24 || m !in 0..59 || (h==24 && m!=0))return null
        return h*60+m
    }
}
