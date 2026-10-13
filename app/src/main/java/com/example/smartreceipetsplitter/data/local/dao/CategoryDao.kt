package com.example.smartreceipetsplitter.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.smartreceipetsplitter.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?
    @Insert
    suspend fun insert(category: CategoryEntity): Long
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaults(categories: List<CategoryEntity>)
}