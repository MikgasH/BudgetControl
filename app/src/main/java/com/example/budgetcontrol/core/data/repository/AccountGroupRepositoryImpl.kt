package com.example.budgetcontrol.core.data.repository

import com.example.budgetcontrol.core.data.local.database.dao.AccountGroupDao
import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupMemberEntity
import com.example.budgetcontrol.core.data.mapper.toDomain
import com.example.budgetcontrol.core.data.mapper.toEntity
import com.example.budgetcontrol.core.domain.model.AccountGroup
import com.example.budgetcontrol.core.domain.repository.AccountGroupRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountGroupRepositoryImpl @Inject constructor(
    private val accountGroupDao: AccountGroupDao
) : AccountGroupRepository {

    override fun getAllGroups(): Flow<List<AccountGroup>> {
        return combine(
            accountGroupDao.getAllGroups(),
            accountGroupDao.getAllMembers()
        ) { groups, allMembers ->
            val membersByGroup = allMembers.groupBy { it.groupId }
            groups.map { entity ->
                entity.toDomain(
                    memberAccountIds = membersByGroup[entity.id]?.map { it.accountId } ?: emptyList()
                )
            }
        }
    }

    override suspend fun getGroupById(id: String): AccountGroup? {
        val entity = accountGroupDao.getGroupById(id) ?: return null
        val members = accountGroupDao.getMembersForGroup(id)
        return entity.toDomain(members)
    }

    override suspend fun insertGroup(group: AccountGroup) {
        accountGroupDao.insertGroup(group.toEntity())
        group.memberAccountIds.forEach { accountId ->
            accountGroupDao.addMember(AccountGroupMemberEntity(group.id, accountId))
        }
    }

    override suspend fun updateGroup(group: AccountGroup) {
        accountGroupDao.updateGroup(group.toEntity())
        accountGroupDao.removeAllMembers(group.id)
        group.memberAccountIds.forEach { accountId ->
            accountGroupDao.addMember(AccountGroupMemberEntity(group.id, accountId))
        }
    }

    override suspend fun deleteGroup(group: AccountGroup) {
        accountGroupDao.deleteGroup(group.toEntity())
    }
}
