package com.example.budgetcontrol.core.data.mapper

import com.example.budgetcontrol.core.data.local.database.entities.AccountGroupEntity
import com.example.budgetcontrol.core.domain.model.AccountGroup

fun AccountGroupEntity.toDomain(memberAccountIds: List<String>): AccountGroup {
    return AccountGroup(
        id = id,
        name = name,
        memberAccountIds = memberAccountIds,
        createdAt = createdAt
    )
}

fun AccountGroup.toEntity(): AccountGroupEntity {
    return AccountGroupEntity(
        id = id,
        name = name,
        createdAt = createdAt
    )
}
