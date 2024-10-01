package com.c0x12c.featureflag.models

import com.c0x12c.featureflag.serializer.CustomDurationSerializer
import com.c0x12c.featureflag.serializer.InstantSerializer
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import kotlinx.serialization.Serializable
import org.apache.maven.artifact.versioning.ComparableVersion
import utils.PercentageMatchingUtil

@Serializable
sealed class MetadataContent {
  abstract fun extractMetadata(key: String): String?

  abstract fun isEnabled(context: Map<String, Any>): Boolean

  @Serializable
  data class UserTargeting(
    val whitelistedUsers: Map<String, Boolean> = emptyMap(),
    val blacklistedUsers: Map<String, Boolean> = emptyMap(),
    val targetedUserIds: List<String> = emptyList(),
    val percentage: Double,
    val defaultValue: Boolean = false
  ) : MetadataContent() {
    init {
      require(percentage in 0.0..100.0) { "Percentage must be between 0 and 100" }
    }

    override fun extractMetadata(key: String): String? =
      when (key) {
        "whitelistedUsers" -> whitelistedUsers.entries.joinToString(",") { "${it.key}:${it.value}" }
        "blacklistedUsers" -> blacklistedUsers.entries.joinToString(",") { "${it.key}:${it.value}" }
        "targetedUserIds" -> targetedUserIds.joinToString(",")
        "percentage" -> percentage.toString()
        "defaultValue" -> defaultValue.toString()
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val userId = context["userId"] as? String ?: return false

      // Check blacklist first
      blacklistedUsers[userId]?.let { return it }

      // Then check whitelist
      whitelistedUsers[userId]?.let { return it }

      // Finally check targeted users or percentage
      return if (userId in targetedUserIds &&
        PercentageMatchingUtil.isTargetedBasedOnPercentage(value = userId, percentage = percentage)
      ) {
        true
      } else {
        defaultValue
      }
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

    override fun extractMetadata(key: String): String? =
      when (key) {
        "groupIds" -> groupIds.joinToString(",")
        "percentage" -> percentage.toString()
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val groupId = context["groupId"] as? String ?: return false
      return groupId in groupIds &&
        PercentageMatchingUtil.isTargetedBasedOnPercentage(groupId, percentage)
    }
  }

  @Serializable
  data class TimeBasedActivation(
    @Serializable(with = InstantSerializer::class)
    val startTime: Instant,
    @Serializable(with = InstantSerializer::class)
    val endTime: Instant
  ) : MetadataContent() {
    override fun extractMetadata(key: String): String? =
      when (key) {
        "startTime" -> startTime.toString()
        "endTime" -> endTime.toString()
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val now = Instant.now()
      return now.isAfter(startTime) &&
        now.isBefore(endTime)
    }
  }

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

    override fun extractMetadata(key: String): String? =
      when (key) {
        "startPercentage" -> startPercentage.toString()
        "endPercentage" -> endPercentage.toString()
        "startTime" -> startTime.toString()
        "duration" -> duration.toString()
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val userId = context["userId"] as? String ?: return false
      val userHash = PercentageMatchingUtil.murmur128x64(value = userId).first.absoluteValue
      val now = Instant.now()

      when {
        now.isBefore(startTime) -> return userHash % 100 < startPercentage
        now.isAfter(startTime.plus(duration)) -> return userHash % 100 < endPercentage
        else -> {
          val elapsedTime = Duration.between(startTime, now)
          val percentage = startPercentage + (endPercentage - startPercentage) * elapsedTime.toMillis().toDouble() / duration.toMillis()
          return userHash % 100 < percentage
        }
      }
    }
  }

  @Serializable
  data class ABTestingConfig(
    val variantA: String,
    val variantB: String,
    // Percentage for variant A, (100 - distribution) for variant B
    val distribution: Double
  ) : MetadataContent() {
    init {
      require(distribution in 0.0..100.0) { "Distribution must be between 0 and 100" }
    }

    override fun extractMetadata(key: String): String? =
      when (key) {
        "variantA" -> variantA
        "variantB" -> variantB
        "distribution" -> distribution.toString()
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val userId = context["userId"] as? String ?: return false
      val userHash = PercentageMatchingUtil.murmur128x64(value = userId).first.absoluteValue
      return userHash % 100 < distribution
    }
  }

  @Serializable
  data class VersionTargeting(
    val minVersion: String,
    val maxVersion: String
  ) : MetadataContent() {
    override fun extractMetadata(key: String): String? =
      when (key) {
        "minVersion" -> minVersion
        "maxVersion" -> maxVersion
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val version = context["appVersion"] as? String ?: return false
      val currentVersion = ComparableVersion(version)
      val minVersion = ComparableVersion(minVersion)
      val maxVersion = ComparableVersion(maxVersion)
      return currentVersion in minVersion..maxVersion
    }
  }

  @Serializable
  data class GeographicTargeting(
    val countries: List<String>,
    val regions: List<String>
  ) : MetadataContent() {
    override fun extractMetadata(key: String): String? =
      when (key) {
        "countries" -> countries.joinToString(",")
        "regions" -> regions.joinToString(",")
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val country = context["country"] as? String
      val region = context["region"] as? String
      val checkBoth = context["checkBoth"] as? Boolean ?: false

      return if (checkBoth) {
        (country != null && country in countries) &&
          (region != null && region in regions)
      } else {
        (country != null && country in countries) ||
          (region != null && region in regions)
      }
    }
  }

  @Serializable
  data class DeviceTargeting(
    // e.g., "iOS", "Android", "Web"
    val platforms: List<String>,
    // e.g., "Mobile", "Tablet", "Desktop"
    val deviceTypes: List<String>
  ) : MetadataContent() {
    override fun extractMetadata(key: String): String? =
      when (key) {
        "platforms" -> platforms.joinToString(",")
        "deviceTypes" -> deviceTypes.joinToString(",")
        else -> null
      }

    override fun isEnabled(context: Map<String, Any>): Boolean {
      val platform = context["platform"] as? String
      val deviceType = context["deviceType"] as? String
      val checkBoth = context["checkBoth"] as? Boolean ?: false

      return if (checkBoth) {
        (platform != null && platform in platforms) &&
          (deviceType != null && deviceType in deviceTypes)
      } else {
        (platform != null && platform in platforms) ||
          (deviceType != null && deviceType in deviceTypes)
      }
    }
  }

  @Serializable
  data class CustomRules(
    // Custom key-value pairs for specific business logic
    val rules: Map<String, String>
  ) : MetadataContent() {
    override fun extractMetadata(key: String): String? = rules[key]

    override fun isEnabled(context: Map<String, Any>): Boolean =
      rules.all { (key, value) ->
        context[key]?.toString()?.equals(value, ignoreCase = true) == true
      }
  }
}
