package com.c0x12c.featureflag.entity

import com.c0x12c.featureflag.serializer.InstantSerializer
import com.c0x12c.featureflag.serializer.UuidSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class FeatureFlag(
    @Serializable(with = UuidSerializer::class)
    var id: UUID = UUID.randomUUID(),
    val name: String,
    val code: String,
    val description: String? = null,
    var enabled: Boolean = false,
    @Contextual
    val metadata: Map<String, @Contextual Any>? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    var updatedAt: Instant? = null
)
