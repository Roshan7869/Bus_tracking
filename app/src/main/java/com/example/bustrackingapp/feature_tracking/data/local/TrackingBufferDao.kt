package com.example.bustrackingapp.feature_tracking.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TrackingBufferDao {

    /** Insert a single cached packet; Room generates the auto-incremented id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TrackingLogEntity)

    /**
     * Retrieve all buffered packets ordered oldest-first so they are synced
     * in the correct chronological sequence.
     */
    @Query("SELECT * FROM tracking_buffer ORDER BY timestampUnix ASC")
    suspend fun getAll(): List<TrackingLogEntity>

    /** Delete the N oldest rows after a successful bulk sync. */
    @Query("""
        DELETE FROM tracking_buffer
        WHERE id IN (
            SELECT id FROM tracking_buffer
            ORDER BY timestampUnix ASC
            LIMIT :count
        )
    """)
    suspend fun deleteOldest(count: Int)

    /** Hard cap: evict oldest row when buffer exceeds [MAX_RECORDS]. */
    @Query("SELECT COUNT(*) FROM tracking_buffer")
    suspend fun count(): Int

    @Query("""
        DELETE FROM tracking_buffer
        WHERE id = (SELECT id FROM tracking_buffer ORDER BY timestampUnix ASC LIMIT 1)
    """)
    suspend fun deleteOldestOne()
}
