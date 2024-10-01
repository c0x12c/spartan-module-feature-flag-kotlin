package com.c0x12c.featureflag.table

import com.c0x12c.featureflag.models.FeatureFlagType
import java.time.Instant
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp

object FeatureFlagTable : UUIDTable("feature_flags") {
  val name = text("name")
  val code = varchar("code", 50)
  val description = text("description").nullable()
  val enabled = bool("enabled")
  val type = customEnumeration(name = "type", sql = "varchar(50)", fromDb = { value -> FeatureFlagType.valueOf(value as String) }, toDb = { it.toString() })
  val metadata = text("metadata").nullable() // Store serialized MetadataContent as JSON
  val createdAt = timestamp("created_at").default(Instant.now())
  val updatedAt = timestamp("updated_at").nullable()
  val deletedAt = timestamp("deleted_at").nullable()

  init {
    index(true, metadata)
    index(true, type)
  }
}
