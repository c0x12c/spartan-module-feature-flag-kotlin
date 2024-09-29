package com.c0x12c.featureflag.service

import RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.exception.NotifierError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.goncalossilva.murmurhash.MurmurHash3
import java.time.Duration
import java.time.Instant
import kotlin.math.absoluteValue
import org.apache.maven.artifact.versioning.ComparableVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service class for managing feature flags.
 *
 * @property repository The repository for feature flag data access.
 * @property cache Optional Redis cache for feature flags.
 */
class FeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val slackNotifier: SlackNotifier? = null
) {
  private companion object {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    private const val DEFAULT_LIMIT = 100
    private const val DEFAULT_OFFSET = 0
  }

  /**
   * Creates a new feature flag.
   *
   * @param featureFlag The feature flag to create.
   * @return The created feature flag.
   * @throws FeatureFlagError If the created flag cannot be retrieved.
   */
  fun createFeatureFlag(featureFlag: FeatureFlag): FeatureFlag {
    logger.info("Creating new feature flag with code: ${featureFlag.code}")

    val createdFlagId = repository.insert(featureFlag)
    val newFlag =
      repository.getById(createdFlagId) ?: run {
        throw FeatureFlagError("Failed to retrieve created flag")
      }
    cache?.set(newFlag.code, newFlag)
    slackNotifier?.send(newFlag, ChangeStatus.CREATED)

    logger.info("Successfully created the feature flag ${newFlag.code}")
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
    logger.info("Retrieving feature flag with code: $code")

    cache?.get(code)?.let {
      logger.info("Found feature flag in cache with code: $code")
      return it
    }

    val featureFlag =
      repository.getByCode(code) ?: run {
        throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
      }
    cache?.set(code, featureFlag)

    logger.info("Successfully retrieved the feature flag ${featureFlag.code}")
    return featureFlag
  }

  /**
   * Enables a feature flag.
   *
   * @param code The code of the feature flag to enable.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun enableFeatureFlag(code: String): FeatureFlag {
    logger.info("Enabling feature flag with code: $code")

    val updatedFlag =
      repository.updateEnableStatus(code, true) ?: run {
        throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
      }

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.ENABLED)

    logger.info("Successfully enabled the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  /**
   * Disables a feature flag.
   *
   * @param code The code of the feature flag to disable.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun disableFeatureFlag(code: String): FeatureFlag {
    logger.info("Disabling feature flag with code: $code")

    val updatedFlag =
      repository.updateEnableStatus(code, false) ?: run {
        throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
      }

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.DISABLED)

    logger.info("Successfully disabled the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  /**
   * Updates an existing feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param featureFlag The updated feature flag data.
   * @return The updated feature flag.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun updateFeatureFlag(
    code: String,
    featureFlag: FeatureFlag
  ): FeatureFlag {
    logger.info("Updating feature flag with code: $code")

    val updatedFlag =
      repository.update(code, featureFlag) ?: run {
        throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
      }

    cache?.set(code, updatedFlag)
    sendNotification(updatedFlag, ChangeStatus.UPDATED)

    logger.info("Successfully updated the feature flag ${updatedFlag.code}")
    return updatedFlag
  }

  /**
   * Deletes a feature flag.
   *
   * @param code The code of the feature flag to delete.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun deleteFeatureFlag(code: String) {
    logger.info("Deleting feature flag with code: $code")

    val result =
      repository.delete(code) ?: run {
        throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
      }
    cache?.delete(code)
    sendNotification(result, ChangeStatus.DELETED)

    logger.info("Successfully deleted the feature flag ${result.code}")
  }

  /**
   * Lists feature flags with pagination.
   *
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags.
   */
  fun listFeatureFlags(
    limit: Int = DEFAULT_LIMIT,
    offset: Int = DEFAULT_OFFSET
  ): List<FeatureFlag> = repository.list(limit, offset)

  /**
   * Finds feature flags by metadata type.
   *
   * @param type The metadata type to search for.
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags matching the metadata type.
   */
  fun findFeatureFlagsByMetadataType(
    type: String,
    limit: Int = DEFAULT_LIMIT,
    offset: Int = DEFAULT_OFFSET
  ): List<FeatureFlag> = repository.findByMetadataType(type, limit, offset)

  /**
   * Checks if a feature flag is enabled for the given context.
   *
   * @param code The code of the feature flag.
   * @param context The context for evaluating the feature flag.
   * @return True if the feature flag is enabled, false otherwise.
   */
  fun isFeatureFlagEnabled(
    code: String,
    context: Map<String, Any>
  ): Boolean {
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

  fun getMetadataValue(
    code: String,
    key: String
  ): String? {
    val result = getFeatureFlagByCode(code)
    return result.metadata?.extractMetadata(key)
  }

  private fun sendNotification(
    featureFlag: FeatureFlag,
    changeStatus: ChangeStatus
  ) {
    try {
      slackNotifier?.send(featureFlag, changeStatus)
    } catch (e: Exception) {
      throw NotifierError("Failed to send notification for feature flag ${featureFlag.code}", e)
    }
  }

  /**
   * Checks if the feature flag is enabled for the given group.
   */
  private fun isGroupTargeted(
    metadata: MetadataContent.GroupTargeting,
    context: Map<String, Any>
  ): Boolean {
    val groupId = context["groupId"] as? String ?: return false
    return groupId in metadata.groupIds && isTargetedBasedOnPercentage(groupId, metadata.percentage)
  }

  /**
   * Checks if the feature flag is enabled for the given user.
   */
  private fun isUserTargeted(
    metadata: MetadataContent.UserTargeting,
    context: Map<String, Any>
  ): Boolean {
    val userId = context["userId"] as? String ?: return false

    // Check blacklist first
    metadata.blacklistedUsers[userId]?.let { return it }

    // Then check whitelist
    metadata.whitelistedUsers[userId]?.let { return it }

    // Finally check targeted users or percentage
    return if (userId in metadata.targetedUserIds && isTargetedBasedOnPercentage(userId, metadata.percentage)) {
      true
    } else {
      metadata.defaultValue
    }
  }

  /**
   * Calculates if the feature flag should be activated based on the percentage and a hash of the identifier.
   */
  private fun isTargetedBasedOnPercentage(
    identifier: String,
    percentage: Double
  ): Boolean {
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
  private fun isGraduallyRolledOut(
    metadata: MetadataContent.GradualRollout,
    context: Map<String, Any>
  ): Boolean {
    val userId = context["userId"] as? String ?: return false
    val userHash = userId.murmur128x64().first.absoluteValue
    val now = Instant.now()

    when {
      now.isBefore(metadata.startTime) -> return userHash % 100 < metadata.startPercentage
      now.isAfter(metadata.startTime.plus(metadata.duration)) -> return userHash % 100 < metadata.endPercentage
      else -> {
        val elapsedTime = Duration.between(metadata.startTime, now)
        val percentage = metadata.startPercentage + (metadata.endPercentage - metadata.startPercentage) * elapsedTime.toMillis().toDouble() / metadata.duration.toMillis()
        return userHash % 100 < percentage
      }
    }
  }

  /**
   * Checks if the feature flag is enabled for A/B testing.
   */
  private fun isABTestingEnabled(
    metadata: MetadataContent.ABTestingConfig,
    context: Map<String, Any>
  ): Boolean {
    val userId = context["userId"] as? String ?: return false
    val userHash = userId.murmur128x64().first.absoluteValue
    return userHash % 100 < metadata.distribution
  }

  /**
   * Checks if the feature flag is enabled for the given app version.
   */
  private fun isVersionTargeted(
    metadata: MetadataContent.VersionTargeting,
    context: Map<String, Any>
  ): Boolean {
    val version = context["appVersion"] as? String ?: return false
    val currentVersion = ComparableVersion(version)
    val minVersion = ComparableVersion(metadata.minVersion)
    val maxVersion = ComparableVersion(metadata.maxVersion)
    return currentVersion in minVersion..maxVersion
  }

  /**
   * Checks if the feature flag is enabled for the given geographic location.
   */
  private fun isGeographicallyTargeted(
    metadata: MetadataContent.GeographicTargeting,
    context: Map<String, Any>
  ): Boolean {
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
  private fun isDeviceTargeted(
    metadata: MetadataContent.DeviceTargeting,
    context: Map<String, Any>
  ): Boolean {
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
  private fun areCustomRulesSatisfied(
    metadata: MetadataContent.CustomRules,
    context: Map<String, Any>
  ): Boolean =
    metadata.rules.all { (key, value) ->
      context[key]?.toString()?.equals(value, ignoreCase = true) == true
    }

  /**
   * Calculates MurmurHash for a string.
   */
  private fun String.murmur128x64(): Pair<Long, Long> {
    val hash = MurmurHash3().hash128x64(this.encodeToByteArray())
    return Pair(hash[0].toLong(), hash[1].toLong())
  }
}
