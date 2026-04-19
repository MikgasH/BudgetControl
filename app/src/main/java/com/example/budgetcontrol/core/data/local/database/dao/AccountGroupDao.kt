package com.example.budgetcontrol.core.data.local.database.dao

import androidx.room.*
import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupEntity
import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountGroupDao {

    @Query("SELECT * FROM account_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<AccountGroupEntity>>

    @Query("SELECT * FROM account_groups WHERE id = :id")
    suspend fun getGroupById(id: String): AccountGroupEntity?

    @Query("SELECT accountId FROM account_group_members WHERE groupId = :groupId")
    suspend fun getMembersForGroup(groupId: String): List<String>

    @Query("SELECT accountId FROM account_group_members WHERE groupId = :groupId")
    fun getMembersForGroupFlow(groupId: String): Flow<List<String>>

    @Query("SELECT * FROM account_group_members")
    fun getAllMembers(): Flow<List<AccountGroupMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: AccountGroupEntity)

    @Update
    suspend fun updateGroup(group: AccountGroupEntity)

    @Delete
    suspend fun deleteGroup(group: AccountGroupEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMember(member: AccountGroupMemberEntity)

    @Query("DELETE FROM account_group_members WHERE groupId = :groupId AND accountId = :accountId")
    suspend fun removeMember(groupId: String, accountId: String)

    @Query("DELETE FROM account_group_members WHERE groupId = :groupId")
    suspend fun removeAllMembers(groupId: String)

    @Transaction
    suspend fun insertGroupWithMembers(group: AccountGroupEntity, accountIds: List<String>) {
        insertGroup(group)
        accountIds.forEach { accountId ->
            addMember(AccountGroupMemberEntity(group.id, accountId))
        }
    }

    @Transaction
    suspend fun updateGroupWithMembers(group: AccountGroupEntity, accountIds: List<String>) {
        updateGroup(group)
        removeAllMembers(group.id)
        accountIds.forEach { accountId ->
            addMember(AccountGroupMemberEntity(group.id, accountId))
        }
    }
}
