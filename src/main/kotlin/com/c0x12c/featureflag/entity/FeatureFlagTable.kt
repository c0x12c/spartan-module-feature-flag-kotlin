package com.c0x12c.featureflag.entity

import jsonb
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.UUID

object FeatureFlags : UUIDTable("feature_flags") {
  val name = text("name")
  val code = varchar("code", 50)
  val description = text("description").nullable()
  val enabled = bool("enabled")
  val metadata = jsonb("metadata")
  val createdAt = timestamp("created_at").default(Instant.now())
  val updatedAt = timestamp("updated_at").nullable()
  val deletedAt = timestamp("deleted_at").nullable()
}

class FeatureFlagEntity(id: EntityID<UUID>) : UUIDEntity(id) {
  companion object : UUIDEntityClass<FeatureFlagEntity>(FeatureFlags)

  var name by FeatureFlags.name
  var code by FeatureFlags.code
  var description by FeatureFlags.description
  var enabled by FeatureFlags.enabled
  var metadata by FeatureFlags.metadata
  var createdAt by FeatureFlags.createdAt
  var updatedAt by FeatureFlags.updatedAt
  var deletedAt by FeatureFlags.deletedAt

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
