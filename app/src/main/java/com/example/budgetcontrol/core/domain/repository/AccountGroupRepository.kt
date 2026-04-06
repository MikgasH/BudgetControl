package com.example.budgetcontrol.core.domain.repository

import com.example.budgetcontrol.core.domain.model.AccountGroup
import kotlinx.coroutines.flow.Flow

interface AccountGroupRepository {
    fun getAllGroups(): Flow<List<AccountGroup>>
    suspend fun getGroupById(id: String): AccountGroup?
    suspend fun insertGroup(group: AccountGroup)
    suspend fun updateGroup(group: AccountGroup)
    suspend fun deleteGroup(group: AccountGroup)
}
