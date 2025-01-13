package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.exception.NotifierError
import com.c0x12c.featureflag.models.FeatureFlagType
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.repository.FeatureFlagRepository
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
    offset: Int = DEFAULT_OFFSET,
    keyword: String? = null
  ): List<FeatureFlag> = repository.list(limit, offset, keyword)

  /**
   * Finds feature flags by metadata type.
   *
   * @param type The metadata type to search for.
   * @param limit Maximum number of flags to return.
   * @param offset Number of flags to skip.
   * @return List of feature flags matching the metadata type.
   */
  fun findFeatureFlagsByMetadataType(
    type: FeatureFlagType,
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

    return flag.metadata?.isEnabled(context) ?: true
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
}
