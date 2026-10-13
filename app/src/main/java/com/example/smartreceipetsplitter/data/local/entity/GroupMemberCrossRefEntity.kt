package com.example.smartreceipetsplitter.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(tableName = "group_member_cross_ref", primaryKeys = ["groupId", "personId"],
    foreignKeys =  [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PersonEntity::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE
        )
    ])
data class GroupMemberCrossRefEntity(
    val groupId: Long,
    val personId: Long
)