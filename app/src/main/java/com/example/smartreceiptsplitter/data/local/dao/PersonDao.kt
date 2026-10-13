package com.example.smartreceiptsplitter.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.smartreceiptsplitter.data.local.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM persons ORDER BY name ASC")
    fun getAll(): Flow<List<PersonEntity>>
    @Query("SELECT * FROM persons WHERE isSelf = 1 LIMIT 1")
    suspend fun getSelf(): PersonEntity?
    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun getById(id: Long): PersonEntity?
    @Insert
    suspend fun insert(person: PersonEntity): Long
    @Update
    suspend fun update(person: PersonEntity)
    @Delete
    suspend fun delete(person: PersonEntity)
}
