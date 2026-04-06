package com.example.budgetcontrol.core.data.local.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "account_group_members",
    primaryKeys = ["groupId", "accountId"],
    foreignKeys = [
        ForeignKey(
            entity = AccountGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class AccountGroupMemberEntity(
    val groupId: String,
    val accountId: String
)
