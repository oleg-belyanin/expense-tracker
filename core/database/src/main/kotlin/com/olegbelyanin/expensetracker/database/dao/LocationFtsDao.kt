package com.olegbelyanin.expensetracker.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.olegbelyanin.expensetracker.database.entities.LocationEntity
import com.olegbelyanin.expensetracker.database.entities.LocationFtsEntity

@Dao
interface LocationFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocationFtsEntity)

    @Query("DELETE FROM location_fts WHERE rowid = :rowId")
    suspend fun deleteByRowId(rowId: Int)

    @Query(
        """
        SELECT location.* FROM location
        INNER JOIN location_fts ON location.id = location_fts.rowid
        WHERE location_fts MATCH :match
          AND location.archived_at IS NULL
          AND location.usage_count > 0
        """,
    )
    suspend fun search(match: String): List<LocationEntity>
}
