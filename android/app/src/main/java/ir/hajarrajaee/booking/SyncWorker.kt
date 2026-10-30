package ir.hajarrajaee.booking

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Android may defer periodic work because of battery optimization/Doze. */
class SyncWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters){
    override suspend fun doWork():Result {
        if(!SyncClient.connected(applicationContext))return Result.success()
        return try{
            val report=SyncClient.synchronize(applicationContext)
            if(report.conflicts.isNotEmpty())Result.failure() else Result.success()
        }catch(e:Exception){Result.retry()}
    }
    companion object {
        fun schedule(context:Context){
            val work=PeriodicWorkRequestBuilder<SyncWorker>(15,TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "booking_server_sync",ExistingPeriodicWorkPolicy.KEEP,work)
        }
    }
}
