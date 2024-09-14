package com.c0x12c.featureflag.entity

import com.c0x12c.featureflag.table.FeatureFlagTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class FeatureFlagEntity(id: EntityID<UUID>) : UUIDEntity(id) {
  companion object : UUIDEntityClass<FeatureFlagEntity>(FeatureFlagTable)

  var name by FeatureFlagTable.name
  var code by FeatureFlagTable.code
  var description by FeatureFlagTable.description
  var enabled by FeatureFlagTable.enabled
  var metadata by FeatureFlagTable.metadata
  var createdAt by FeatureFlagTable.createdAt
  var updatedAt by FeatureFlagTable.updatedAt
  var deletedAt by FeatureFlagTable.deletedAt

  fun toFeatureFlag() = FeatureFlagCache(
    id = id.value,
    name = name,
    code = code,
    description = description,
    enabled = enabled,
    metadata = metadata.split(",").associate {
      val (key, value) = it.split(":")
      key to value
    },
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
  )
}