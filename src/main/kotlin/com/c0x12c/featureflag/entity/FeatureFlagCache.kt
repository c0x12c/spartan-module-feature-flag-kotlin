package com.c0x12c.featureflag.entity

import com.c0x12c.featureflag.serializer.InstantSerializer
import com.c0x12c.featureflag.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class FeatureFlagCache(
  @Serializable(with = UuidSerializer::class)
  val id: UUID = UUID.randomUUID(),
  val name: String,
  val code: String,
  val description: String? = null,
  var enabled: Boolean = false,
  val metadata: Map<String, String> = emptyMap(),
  @Serializable(with = InstantSerializer::class)
  val createdAt: Instant = Instant.now(),
  @Serializable(with = InstantSerializer::class)
  var updatedAt: Instant? = null,
  @Serializable(with = InstantSerializer::class)
  var deletedAt: Instant? = null
) {
  fun toEntity(): FeatureFlagEntity {
    return FeatureFlagEntity.new(id) {
      name = this@FeatureFlagCache.name
      code = this@FeatureFlagCache.code
      description = this@FeatureFlagCache.description
      enabled = this@FeatureFlagCache.enabled
      metadata = this@FeatureFlagCache.metadata.entries.joinToString(",") { (k, v) -> "$k:$v" }
      createdAt = this@FeatureFlagCache.createdAt
      updatedAt = this@FeatureFlagCache.updatedAt
      deletedAt = this@FeatureFlagCache.deletedAt
    }
  }
}
