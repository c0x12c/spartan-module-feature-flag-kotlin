package com.c0x12c.featureflag.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

object FeatureFlagTable : UUIDTable("feature_flags") {
  val name = text("name")
  val code = varchar("code", 50)
  val description = text("description").nullable()
  val enabled = bool("enabled")
  val metadata = text("metadata").nullable() // Store serialized MetadataContent as JSON
  val metadataType = varchar("metadata_type", 50).nullable() // Store the type of metadata
  val createdAt = timestamp("created_at").default(Instant.now())
  val updatedAt = timestamp("updated_at").nullable()
  val deletedAt = timestamp("deleted_at").nullable()

  init {
    index(true, metadata)
    index(true, metadataType)
  }
}
