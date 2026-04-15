package com.resilience.app.workers

import android.content.Context
import androidx.work.*
import com.resilience.app.data.db.ResilienceDatabase
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * WorkManager task that prunes offline tile packs older than [STALE_DAYS] days.
 *
 * Both the MapLibre tile store and the Room metadata entry are removed so
 * they stay in sync.  Runs weekly when the device is idle and charging.
 *
 * Schedule from your Application or first MapScreen launch via:
 *   MapTileCleanupWorker.schedule(context)
 */
class MapTileCleanupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val WORK_NAME  = "map_tile_cleanup"
        private const val STALE_DAYS = 90L

        /**
         * Enqueues a unique periodic cleanup job.
         * Safe to call multiple times — WorkManager deduplicates by [WORK_NAME].
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MapTileCleanupWorker>(7, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresDeviceIdle(true)
                        .setRequiresCharging(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val cutoffMillis = System.currentTimeMillis() - STALE_DAYS * 24 * 60 * 60 * 1000L

            // Open DB directly — no Hilt injection needed in this worker
            val db = ResilienceDatabase.create(context)
            val dao = db.offlineRegionDao()
            val staleRegions = dao.getRegionsOlderThan(cutoffMillis)

            if (staleRegions.isEmpty()) return Result.success()

            // Init MapLibre so OfflineManager is accessible
            MapLibre.getInstance(context)
            val offlineManager = OfflineManager.getInstance(context)
            val liveRegions = listRegions(offlineManager)

            var deleted = 0
            for (stale in staleRegions) {
                val liveRegion = liveRegions.find { it.id == stale.id }
                if (liveRegion != null) {
                    val ok = deleteRegion(liveRegion)
                    if (ok) {
                        dao.deleteById(stale.id)
                        deleted++
                    }
                } else {
                    // Orphaned Room entry — remove it
                    dao.deleteById(stale.id)
                    deleted++
                }
            }

            db.close()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun listRegions(manager: OfflineManager): List<OfflineRegion> =
        suspendCancellableCoroutine { cont ->
            manager.listOfflineRegions(object : OfflineManager.ListOfflineRegionsCallback {
                override fun onList(regions: Array<OfflineRegion>?) =
                    cont.resume(regions?.toList() ?: emptyList())
                override fun onError(error: String) = cont.resume(emptyList())
            })
        }

    private suspend fun deleteRegion(region: OfflineRegion): Boolean =
        suspendCancellableCoroutine { cont ->
            region.delete(object : OfflineRegion.OfflineRegionDeleteCallback {
                override fun onDelete() = cont.resume(true)
                override fun onError(error: String) = cont.resume(false)
            })
        }
}
