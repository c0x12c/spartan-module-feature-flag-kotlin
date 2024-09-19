package com.c0x12c.featureflag.models

import com.c0x12c.featureflag.serializer.CustomDurationSerializer
import com.c0x12c.featureflag.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant

@Serializable
sealed class MetadataContent {
  @Serializable
  data class UserTargeting(
    val userIds: List<String>,
    val percentage: Double
  ) : MetadataContent() {
    init {
      require(percentage in 0.0..100.0) { "Percentage must be between 0 and 100" }
    }
  }

  @Serializable
  data class GroupTargeting(
    val groupIds: List<String>,
    val percentage: Double
  ) : MetadataContent() {
    init {
      require(percentage in 0.0..100.0) { "Percentage must be between 0 and 100" }
    }
  }

  @Serializable
  data class TimeBasedActivation(
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val endTime: Instant
  ) : MetadataContent()

  @Serializable
  data class GradualRollout(
    val startPercentage: Double,
    val endPercentage: Double,
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant,
    @Serializable(with = CustomDurationSerializer::class)
    val duration: Duration
  ) : MetadataContent() {
    init {
      require(startPercentage in 0.0..100.0) { "Start percentage must be between 0 and 100" }
      require(endPercentage in 0.0..100.0) { "End percentage must be between 0 and 100" }
    }
  }

  @Serializable
  data class ABTestingConfig(
    val variantA: String,
    val variantB: String,
    val distribution: Double // Percentage for variant A, (100 - distribution) for variant B
  ) : MetadataContent() {
    init {
      require(distribution in 0.0..100.0) { "Distribution must be between 0 and 100" }
    }
  }

  @Serializable
  data class VersionTargeting(
    val minVersion: String,
    val maxVersion: String
  ) : MetadataContent()

  @Serializable
  data class GeographicTargeting(
    val countries: List<String>,
    val regions: List<String>
  ) : MetadataContent()

  @Serializable
  data class DeviceTargeting(
    val platforms: List<String>, // e.g., "iOS", "Android", "Web"
    val deviceTypes: List<String> // e.g., "Mobile", "Tablet", "Desktop"
  ) : MetadataContent()

  @Serializable
  data class CustomRules(
    val rules: Map<String, String> // Custom key-value pairs for specific business logic
  ) : MetadataContent()
}
