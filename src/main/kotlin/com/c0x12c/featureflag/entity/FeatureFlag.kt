package com.c0x12c.featureflag.entity

import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.serializer.InstantSerializer
import com.c0x12c.featureflag.serializer.UuidSerializer
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class FeatureFlag(
  @Serializable(with = UuidSerializer::class)
  val id: UUID = UUID.randomUUID(),
  val name: String,
  val code: String,
  val description: String? = null,
  val enabled: Boolean = false,
  val metadata: MetadataContent? = null,
  @Serializable(with = InstantSerializer::class)
  val createdAt: Instant = Instant.now(),
  @Serializable(with = InstantSerializer::class)
  val updatedAt: Instant? = null,
  @Serializable(with = InstantSerializer::class)
  val deletedAt: Instant? = null
)
