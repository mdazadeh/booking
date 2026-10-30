package ir.hajarrajaee.booking

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class LocalClient(val id:Long,val name:String,val phone:String,val history:String)
data class LocalNote(val id:Long,val clientId:Long,val sessionId:Long?,val body:String,val createdAt:Long)
data class LocalSession(
    val id:Long,val clientId:Long,val startMillis:Long,val minutes:Int,
    val status:String,val series:String?,val topic:String,
    val syncKey:String="",val remoteId:Long?=null,val remoteVersion:Int=0,val dirty:Boolean=true
)

class BookingStore(context:Context):SQLiteOpenHelper(context,"booking_local_1_2.db",null,2) {
    override fun onOpen(db:SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("CREATE TABLE IF NOT EXISTS security_meta (id INTEGER PRIMARY KEY, done INTEGER NOT NULL)")
        db.rawQuery("SELECT done FROM security_meta WHERE id=1",null).use{if(it.moveToFirst() && it.getInt(0)==1)return}
        db.beginTransaction()
        try {
            for(table in listOf("clients","notes","sessions")){
                val columns=when(table){
                    "clients"->listOf("name","phone","history")
                    "notes"->listOf("body")
                    else->listOf("topic")
                }
                val changes=mutableListOf<Pair<Long,ContentValues>>()
                db.rawQuery("SELECT id,"+columns.joinToString(",")+" FROM "+table,null).use { c ->
                    while(c.moveToNext()){
                        val values=ContentValues()
                        columns.forEachIndexed { index,key ->
                            val old=c.getString(index+1) ?: ""
                            if(old.isNotEmpty() && !old.startsWith("enc:v1:"))
                                values.put(key,FieldCrypto.encrypt(old))
                        }
                        if(values.size()>0)changes.add(c.getLong(0) to values)
                    }
                }
                changes.forEach{ (id,values) ->
                    db.update(table,values,"id=?",arrayOf(id.toString()))
                }
            }
            db.execSQL("INSERT OR REPLACE INTO security_meta(id,done) VALUES(1,1)")
            db.setTransactionSuccessful()
        } finally {db.endTransaction()}
    }
    override fun onConfigure(db:SQLiteDatabase){db.setForeignKeyConstraintsEnabled(true)}
    override fun onCreate(db:SQLiteDatabase){
        db.execSQL("CREATE TABLE clients(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT NOT NULL DEFAULT '',history TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE sessions(id INTEGER PRIMARY KEY AUTOINCREMENT,client_id INTEGER NOT NULL REFERENCES clients(id),start_millis INTEGER NOT NULL,minutes INTEGER NOT NULL DEFAULT 60,status TEXT NOT NULL DEFAULT 'approved',series TEXT,topic TEXT NOT NULL DEFAULT '',
            sync_key TEXT UNIQUE,remote_id INTEGER UNIQUE,remote_version INTEGER NOT NULL DEFAULT 0,
            dirty INTEGER NOT NULL DEFAULT 1)")
        db.execSQL("CREATE INDEX session_time ON sessions(start_millis)")
        db.execSQL("CREATE INDEX session_series ON sessions(series)")
        db.execSQL("CREATE INDEX session_client ON sessions(client_id)")
        db.execSQL("CREATE TABLE notes(id INTEGER PRIMARY KEY AUTOINCREMENT,client_id INTEGER NOT NULL REFERENCES clients(id),session_id INTEGER REFERENCES sessions(id) ON DELETE SET NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX note_client ON notes(client_id,created_at)")
    }
    override fun onUpgrade(db:SQLiteDatabase,old:Int,new:Int) {
        if(old<2){
            db.execSQL("ALTER TABLE sessions ADD COLUMN sync_key TEXT")
            db.execSQL("ALTER TABLE sessions ADD COLUMN remote_id INTEGER")
            db.execSQL("ALTER TABLE sessions ADD COLUMN remote_version INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE sessions ADD COLUMN dirty INTEGER NOT NULL DEFAULT 1")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_local_sync_key ON sessions(sync_key)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_local_remote_id ON sessions(remote_id)")
            db.execSQL("UPDATE sessions SET sync_key=lower(hex(randomblob(16))) WHERE sync_key IS NULL")
        }
    }
    fun clients():List<LocalClient>{
        val out=mutableListOf<LocalClient>()
        readableDatabase.rawQuery("SELECT id,name,phone,history FROM clients ORDER BY name",null).use{c->
            while(c.moveToNext())out.add(LocalClient(c.getLong(0),FieldCrypto.decrypt(c.getString(1)),FieldCrypto.decrypt(c.getString(2)),FieldCrypto.decrypt(c.getString(3))))
        }
        return out
    }
    fun sessions():List<LocalSession>{
        val out=mutableListOf<LocalSession>()
        readableDatabase.rawQuery("SELECT id,client_id,start_millis,minutes,status,series,topic,sync_key,remote_id,remote_version,dirty FROM sessions ORDER BY start_millis",null).use{c->
            while(c.moveToNext())out.add(LocalSession(c.getLong(0),c.getLong(1),c.getLong(2),c.getInt(3),c.getString(4),c.getString(5),FieldCrypto.decrypt(c.getString(6)),
                c.getString(7) ?: "",if(c.isNull(8))null else c.getLong(8),c.getInt(9),c.getInt(10)!=0))
        }
        return out
    }
    fun notes(clientId:Long):List<LocalNote>{
        val out=mutableListOf<LocalNote>()
        readableDatabase.rawQuery("SELECT id,client_id,session_id,body,created_at FROM notes WHERE client_id=? ORDER BY created_at DESC",arrayOf(clientId.toString())).use{c->
            while(c.moveToNext())out.add(LocalNote(c.getLong(0),c.getLong(1),if(c.isNull(2))null else c.getLong(2),FieldCrypto.decrypt(c.getString(3)),c.getLong(4)))
        }
        return out
    }
    fun saveClient(id:Long?,name:String,phone:String,history:String):Long {
        require(name.trim().isNotBlank()&&name.length<=160&&phone.length<=40&&history.length<=12000)
        val v=ContentValues().apply{put("name",FieldCrypto.encrypt(name.trim()));put("phone",FieldCrypto.encrypt(phone.trim()));put("history",FieldCrypto.encrypt(history.trim()))}
        if(id==null)return writableDatabase.insertOrThrow("clients",null,v)
        if(writableDatabase.update("clients",v,"id=?",arrayOf(id.toString()))!=1)throw IllegalArgumentException("پرونده یافت نشد")
        return id
    }
    fun addNote(clientId:Long,sessionId:Long?,body:String){
        require(body.isNotBlank()&&body.length<=20000)
        val v=ContentValues().apply{
            put("client_id",clientId);put("body",FieldCrypto.encrypt(body.trim()));put("created_at",System.currentTimeMillis())
            if(sessionId!=null)put("session_id",sessionId)
        }
        writableDatabase.insertOrThrow("notes",null,v)
    }
    fun removeNote(noteId:Long){writableDatabase.delete("notes","id=?",arrayOf(noteId.toString()))}
    private fun conflict(db:SQLiteDatabase,start:Long,minutes:Int,excluded:Set<Long>):Boolean{
        val end=start+minutes*60000L
        val where=if(excluded.isEmpty())"" else " AND id NOT IN ("+excluded.joinToString(","){"?"}+")"
        val args=(listOf(end.toString(),start.toString())+excluded.map{it.toString()}).toTypedArray()
        db.rawQuery("SELECT id FROM sessions WHERE status='approved' AND start_millis<? AND start_millis+minutes*60000>?"+where+" LIMIT 1",args).use{
            return it.moveToFirst()
        }
    }
    fun addSessions(clientId:Long,date:LocalDate,time:LocalTime,minutes:Int,repeat:Repeat,count:Int,
                    custom:Set<LocalDate>,topic:String):Int {
        require(minutes in 5..480 && topic.length<=1000)
        val dates=recurrenceDates(date,repeat,count,custom)
        require(dates.isNotEmpty())
        val series=if(dates.size>1)UUID.randomUUID().toString() else null
        val db=writableDatabase
        db.beginTransaction()
        try {
            for(day in dates){
                val start=Jalali.millis(day,time)
                if(conflict(db,start,minutes,emptySet()))throw IllegalArgumentException("تداخل در "+Jalali.label(day))
                val v=ContentValues().apply{
                    put("client_id",clientId);put("start_millis",start);put("minutes",minutes)
                    put("status","approved");put("series",series);put("topic",FieldCrypto.encrypt(topic.trim()))
                    put("sync_key",UUID.randomUUID().toString());put("dirty",1)
                }
                db.insertOrThrow("sessions",null,v)
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
        return dates.size
    }
    fun editSession(original:LocalSession,newStart:Long,newMinutes:Int,allFuture:Boolean){
        require(newMinutes in 5..480)
        val affected=if(allFuture && original.series!=null)
            sessions().filter{it.series==original.series&&it.startMillis>=original.startMillis&&it.status=="approved"}
            else listOf(original)
        val excluded=affected.map{it.id}.toSet()
        val delta=newStart-original.startMillis
        val starts=affected.map{it.startMillis+delta}
        val db=writableDatabase
        db.beginTransaction()
        try{
            affected.forEachIndexed{index,item->
                val start=starts[index]
                if(conflict(db,start,newMinutes,excluded) || starts.indices.any{j->
                        j!=index && start < starts[j]+newMinutes*60000L &&
                          start+newMinutes*60000L>starts[j]
                    })throw IllegalArgumentException("تداخل هنگام جابه‌جایی مجموعه")
                val v=ContentValues().apply{put("start_millis",start);put("minutes",newMinutes);put("dirty",1)}
                db.update("sessions",v,"id=?",arrayOf(item.id.toString()))
            }
            db.setTransactionSuccessful()
        }finally{db.endTransaction()}
    }
    fun deleteSession(session:LocalSession,allFuture:Boolean) {
        if(allFuture&&session.series!=null)
            writableDatabase.update("sessions",ContentValues().apply{put("status","cancelled");put("dirty",1)},
                "series=? AND start_millis>=?",arrayOf(session.series,session.startMillis.toString()))
        else writableDatabase.update("sessions",ContentValues().apply{put("status","cancelled");put("dirty",1)},
                "id=?",arrayOf(session.id.toString()))
    }
    fun dirtySessions():List<LocalSession> = sessions().filter{it.dirty}
    fun markSynced(id:Long,remoteId:Long,version:Int) {
        val v=ContentValues().apply{put("remote_id",remoteId);put("remote_version",version);put("dirty",0)}
        writableDatabase.update("sessions",v,"id=?",arrayOf(id.toString()))
    }
    /** Never overwrite unsynced local edits with remote state. */
    fun importRemote(remoteId:Long,key:String?,version:Int,name:String,phone:String,
                     start:Long,minutes:Int,status:String):Boolean {
        require(remoteId>0 && version>0 && minutes in 5..480 && name.isNotBlank())
        val current=sessions()
        val existing=current.firstOrNull{it.remoteId==remoteId || (key!=null && it.syncKey==key)}
        if(existing!=null && existing.dirty)return false
        if(existing!=null && existing.remoteVersion>=version)return true
        val available=clients()
        val client=if(phone.isNotBlank())available.firstOrNull{it.phone==phone}
            else available.firstOrNull{it.name==name}
        val clientId=client?.id ?: saveClient(null,name,phone,"")
        val v=ContentValues().apply{
            put("client_id",clientId);put("start_millis",start);put("minutes",minutes)
            put("status",status);put("sync_key",key?: "server-"+remoteId)
            put("remote_id",remoteId);put("remote_version",version);put("dirty",0)
            put("topic",existing?.let{FieldCrypto.encrypt(it.topic)} ?: "")
        }
        if(existing==null)writableDatabase.insertOrThrow("sessions",null,v)
        else writableDatabase.update("sessions",v,"id=?",arrayOf(existing.id.toString()))
        return true
    }

}
