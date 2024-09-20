package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.goncalossilva.murmurhash.MurmurHash3
import org.apache.maven.artifact.versioning.ComparableVersion
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue

/**
 * Service class for managing feature flags.
 *
 * @property repository The repository for feature flag data access.
 * @property cache Optional Redis cache for feature flags.
 * @property cacheTTLSeconds Time-to-live for cached feature flags in seconds.
 */
class FeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val cacheTTLSeconds: Long = 3600
) {

  /**
   * Creates a new feature flag.
   *
   * @param featureFlag The feature flag to create.
   * @return The created feature flag.
   * @throws FeatureFlagError If the created flag cannot be retrieved.
   */
  fun createFeatureFlag(featureFlag: FeatureFlag): FeatureFlag {
    val createdFlagId = repository.insert(featureFlag)
    val newFlag = repository.getById(createdFlagId) ?: throw FeatureFlagError("Failed to retrieve created flag")
    cache?.set(newFlag.code, newFlag, cacheTTLSeconds)
    return newFlag
  }

  /**
   * Retrieves a feature flag by its code.
   *
   * @param code The code of the feature flag.
   * @return The feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun getFeatureFlagByCode(code: String): FeatureFlag {
    cache?.get(code)?.let { return it }

    val featureFlag = repository.getByCode(code) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, featureFlag, cacheTTLSeconds)
    return featureFlag
  }

  /**
   * Updates an existing feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param featureFlag The updated feature flag data.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun updateFeatureFlag(code: String, featureFlag: FeatureFlag): FeatureFlag {
    val updatedFlag = repository.update(code, featureFlag) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, updatedFlag, cacheTTLSeconds)
    return updatedFlag
  }

  /**
   * Deletes a feature flag.
   *
   * @param code The code of the feature flag to delete.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun deleteFeatureFlag(code: String) {
    if (!repository.delete(code)) {
      throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    }
    cache?.delete(code)
  }

  /**
   * Lists feature flags with pagination.
   *
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags.
   */
  fun listFeatureFlags(limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return repository.list(limit, offset)
  }

  /**
   * Finds feature flags by metadata type.
   *
   * @param type The metadata type to search for.
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags matching the metadata type.
   */
  fun findFeatureFlagsByMetadataType(type: String, limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return repository.findByMetadataType(type, limit, offset)
  }

  /**
   * Checks if a feature flag is enabled for the given context.
   *
   * @param code The code of the feature flag.
   * @param context The context for evaluating the feature flag.
   * @return True if the feature flag is enabled, false otherwise.
   */
  fun isFeatureFlagEnabled(code: String, context: Map<String, Any>): Boolean {
    val flag = getFeatureFlagByCode(code)

    // If the flag is not enabled, return false immediately
    if (!flag.enabled) return false

    // If there's no metadata, return true since the flag is enabled
    val metadata = flag.metadata ?: return true

    return when (metadata) {
      is MetadataContent.UserTargeting -> isUserTargeted(metadata, context)
      is MetadataContent.GroupTargeting -> isGroupTargeted(metadata, context)
      is MetadataContent.TimeBasedActivation -> isTimeBasedActivated(metadata)
      is MetadataContent.GradualRollout -> isGraduallyRolledOut(metadata, context)
      is MetadataContent.ABTestingConfig -> isABTestingEnabled(metadata, context)
      is MetadataContent.VersionTargeting -> isVersionTargeted(metadata, context)
      is MetadataContent.GeographicTargeting -> isGeographicallyTargeted(metadata, context)
      is MetadataContent.DeviceTargeting -> isDeviceTargeted(metadata, context)
      is MetadataContent.CustomRules -> areCustomRulesSatisfied(metadata, context)
    }
  }

  fun getMetadataValue(code: String, key: String): String? {
    return when (val result = getFeatureFlagByCode(code)) {
      else -> {
        when (val metadata = result.metadata) {
          null -> null
          is MetadataContent.UserTargeting -> when (key) {
            "userIds" -> metadata.userIds.joinToString(",")
            "percentage" -> metadata.percentage.toString()
            else -> null
          }

          is MetadataContent.GroupTargeting -> when (key) {
            "groupIds" -> metadata.groupIds.joinToString(",")
            "percentage" -> metadata.percentage.toString()
            else -> null
          }

          is MetadataContent.TimeBasedActivation -> when (key) {
            "startTime" -> metadata.startTime.toString()
            "endTime" -> metadata.endTime.toString()
            else -> null
          }

          is MetadataContent.GradualRollout -> when (key) {
            "startPercentage" -> metadata.startPercentage.toString()
            "endPercentage" -> metadata.endPercentage.toString()
            "startTime" -> metadata.startTime.toString()
            "duration" -> metadata.duration.toString()
            else -> null
          }

          is MetadataContent.ABTestingConfig -> when (key) {
            "variantA" -> metadata.variantA
            "variantB" -> metadata.variantB
            "distribution" -> metadata.distribution.toString()
            else -> null
          }

          is MetadataContent.VersionTargeting -> when (key) {
            "minVersion" -> metadata.minVersion
            "maxVersion" -> metadata.maxVersion
            else -> null
          }

          is MetadataContent.GeographicTargeting -> when (key) {
            "countries" -> metadata.countries.joinToString(",")
            "regions" -> metadata.regions.joinToString(",")
            else -> null
          }

          is MetadataContent.DeviceTargeting -> when (key) {
            "platforms" -> metadata.platforms.joinToString(",")
            "deviceTypes" -> metadata.deviceTypes.joinToString(",")
            else -> null
          }

          is MetadataContent.CustomRules -> metadata.rules[key]
        }
      }
    }
  }

  /**
   * Checks if the feature flag is enabled for the given group.
   */
  private fun isGroupTargeted(metadata: MetadataContent.GroupTargeting, context: Map<String, Any>): Boolean {
    val groupId = context["groupId"] as? String ?: return false
    return groupId in metadata.groupIds && isTargetedBasedOnPercentage(groupId, metadata.percentage)
  }

  /**
   * Checks if the feature flag is enabled for the given user.
   */
  private fun isUserTargeted(metadata: MetadataContent.UserTargeting, context: Map<String, Any>): Boolean {
    val userId = context["userId"] as? String ?: return false
    return userId in metadata.userIds && isTargetedBasedOnPercentage(userId, metadata.percentage)
  }

  /**
   * Calculates if the feature flag should be activated based on the percentage and a hash of the identifier.
   */
  private fun isTargetedBasedOnPercentage(identifier: String, percentage: Double): Boolean {
    val hash = identifier.murmur128x64().first.absoluteValue
    val hashPercentage = (hash % 100) + 1
    return hashPercentage <= percentage
  }

  /**
   * Checks if the feature flag is active based on the current time.
   */
  private fun isTimeBasedActivated(metadata: MetadataContent.TimeBasedActivation): Boolean {
    val now = Instant.now()
    return now.isAfter(metadata.startTime) && now.isBefore(metadata.endTime)
  }

  /**
   * Checks if the feature flag is enabled based on gradual rollout configuration.
   */
  private fun isGraduallyRolledOut(metadata: MetadataContent.GradualRollout, context: Map<String, Any>): Boolean {
    val userId = context["userId"] as? String ?: return false
    val userHash = userId.murmur128x64().first.absoluteValue
    val now = Instant.now()

    when {
      now.isBefore(metadata.startTime) -> return userHash % 100 < metadata.startPercentage
      now.isAfter(metadata.startTime.plus(metadata.duration)) -> return userHash % 100 < metadata.endPercentage
      else -> {
        val elapsedTime = Duration.between(metadata.startTime, now)
        val percentage = metadata.startPercentage + (metadata.endPercentage - metadata.startPercentage) *
          elapsedTime.toMillis().toDouble() / metadata.duration.toMillis()
        return userHash % 100 < percentage
      }
    }
  }

  /**
   * Checks if the feature flag is enabled for A/B testing.
   */
  private fun isABTestingEnabled(metadata: MetadataContent.ABTestingConfig, context: Map<String, Any>): Boolean {
    val userId = context["userId"] as? String ?: return false
    val userHash = userId.murmur128x64().first.absoluteValue
    return userHash % 100 < metadata.distribution
  }

  /**
   * Checks if the feature flag is enabled for the given app version.
   */
  private fun isVersionTargeted(metadata: MetadataContent.VersionTargeting, context: Map<String, Any>): Boolean {
    val version = context["appVersion"] as? String ?: return false
    val currentVersion = ComparableVersion(version)
    val minVersion = ComparableVersion(metadata.minVersion)
    val maxVersion = ComparableVersion(metadata.maxVersion)
    return currentVersion in minVersion..maxVersion
  }

  /**
   * Checks if the feature flag is enabled for the given geographic location.
   */
  private fun isGeographicallyTargeted(metadata: MetadataContent.GeographicTargeting, context: Map<String, Any>): Boolean {
    val country = context["country"] as? String
    val region = context["region"] as? String
    val checkBoth = context["checkBoth"] as? Boolean ?: false

    return if (checkBoth) {
      (country != null && country in metadata.countries) && (region != null && region in metadata.regions)
    } else {
      (country != null && country in metadata.countries) || (region != null && region in metadata.regions)
    }
  }

  /**
   * Checks if the feature flag is enabled for the given device.
   */
  private fun isDeviceTargeted(metadata: MetadataContent.DeviceTargeting, context: Map<String, Any>): Boolean {
    val platform = context["platform"] as? String
    val deviceType = context["deviceType"] as? String
    val checkBoth = context["checkBoth"] as? Boolean ?: false

    return if (checkBoth) {
      (platform != null && platform in metadata.platforms) && (deviceType != null && deviceType in metadata.deviceTypes)
    } else {
      (platform != null && platform in metadata.platforms) || (deviceType != null && deviceType in metadata.deviceTypes)
    }
  }

  /**
   * Checks if the feature flag is enabled based on custom rules.
   */
  private fun areCustomRulesSatisfied(metadata: MetadataContent.CustomRules, context: Map<String, Any>): Boolean {
    return metadata.rules.all { (key, value) ->
      context[key]?.toString()?.equals(value, ignoreCase = true) == true
    }
  }

  /**
   * Calculates MurmurHash for a string.
   */
  private fun String.murmur128x64(): Pair<Long, Long> {
    val hash = MurmurHash3().hash128x64(this.encodeToByteArray())
    return Pair(hash[0].toLong(), hash[1].toLong())
  }
}
