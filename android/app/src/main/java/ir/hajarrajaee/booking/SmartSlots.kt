package ir.hajarrajaee.booking

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.abs

data class SuggestedSlot(val date:LocalDate,val time:LocalTime,val score:Int,val reason:String)

/**
 * Explainable, deterministic recommendations. Use server availability when syncing online.
 * Local recommendations are only provisional until central server confirms capacity.
 */
object SmartSlots {
    fun suggest(
        existing:List<LocalSession>,from:LocalDate,duration:Int,
        days:Int=14,limit:Int=8,dayStart:LocalTime=LocalTime.of(10,0),
        dayEnd:LocalTime=LocalTime.of(21,0)
    ):List<SuggestedSlot> {
        require(duration in 5..480)
        val today=Jalali.today()
        val now=System.currentTimeMillis()
        val list=mutableListOf<SuggestedSlot>()
        for(d in 0 until days.coerceIn(1,60)){
            val date=from.plusDays(d.toLong())
            val sessions=existing.filter{it.status=="approved" && Jalali.time(it.startMillis).toLocalDate()==date}
            var t=dayStart
            while(!t.plusMinutes(duration.toLong()).isAfter(dayEnd)){
                val start=Jalali.millis(date,t);val end=start+duration*60_000L
                if(start>now && sessions.none{start<it.startMillis+it.minutes*60_000L && end>it.startMillis}){
                    val nearest=sessions.minOfOrNull { s ->
                        minOf(abs(start-(s.startMillis+s.minutes*60_000L)),
                            abs(end-s.startMillis))
                    } ?: Long.MAX_VALUE
                    val compact=nearest<15*60_000L
                    val midday=t.hour in 11..18
                    val score=(days-d)*6 + (if(compact)100 else 0) + (if(midday)6 else 0)
                    val reason=if(compact)"بدون فاصله زیاد با جلسه قبلی" else if(d==0)"اولین فرصت امروز" else "زمان آزاد"
                    list.add(SuggestedSlot(date,t,score,reason))
                }
                t=t.plusMinutes(5)
            }
        }
        return list.sortedWith(compareByDescending<SuggestedSlot>{it.score}
            .thenBy{it.date}.thenBy{it.time}).take(limit.coerceIn(1,20))
    }
}
