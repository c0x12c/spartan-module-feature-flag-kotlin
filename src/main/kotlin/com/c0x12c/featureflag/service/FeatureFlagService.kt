package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import java.time.Instant

class FeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val cacheTTLSeconds: Long = 3600
) {
  /**
   * Create a new feature flag.
   *
   * @param flagData Map containing the feature flag data.
   * @return The created FeatureFlagEntity.
   */
  fun createFeatureFlag(flagData: Map<String, Any>): FeatureFlagEntity {
    val featureFlagEntity = FeatureFlagEntity.new {
      name = flagData["name"] as String
      code = flagData["code"] as String
      description = flagData["description"] as? String
      enabled = flagData["enabled"] as? Boolean ?: false
      metadata = parseMetadata(flagData["metadata"])
      createdAt = Instant.now()
    }

    repository.insert(featureFlagEntity)
    cache?.set(featureFlagEntity.code, featureFlagEntity.toFeatureFlag(), cacheTTLSeconds)

    return featureFlagEntity
  }

  /**
   * Retrieve a feature flag by its code.
   *
   * @param code The code of the feature flag.
   * @return The FeatureFlagEntity if found.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun getFeatureFlagByCode(code: String): FeatureFlagEntity {
    cache?.get(code)?.let {
      return it.toEntity()
    }

    val featureFlagEntity = repository.getByCode(code)
    return if (featureFlagEntity != null) {
      cache?.set(code, featureFlagEntity.toFeatureFlag(), cacheTTLSeconds)
      featureFlagEntity
    } else {
      throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    }
  }

  /**
   * Update a feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param flagData Map containing the updated feature flag data.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun updateFeatureFlag(code: String, flagData: Map<String, Any>) {
    val existingFlag = repository.getByCode(code)
      ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    existingFlag.apply {
      name = flagData["name"] as? String ?: name
      description = flagData["description"] as? String ?: description
      enabled = flagData["enabled"] as? Boolean ?: enabled
      metadata = parseMetadata(flagData["metadata"]) ?: metadata
      updatedAt = Instant.now()
    }

    repository.update(existingFlag)
    cache?.set(existingFlag.code, existingFlag.toFeatureFlag(), cacheTTLSeconds)
  }

  /**
   * Delete a feature flag (soft delete).
   *
   * @param code The code of the feature flag to delete.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun deleteFeatureFlag(code: String) {
    val featureFlag = repository.getByCode(code)
      ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")

    featureFlag.apply {
      deletedAt = Instant.now()
    }
    repository.update(featureFlag)
    cache?.delete(code)
  }

  /**
   * List feature flags with optional pagination.
   *
   * @param limit The maximum number of feature flags to return.
   * @param offset The number of feature flags to skip.
   * @return A list of FeatureFlagEntity objects.
   * @throws FeatureFlagError If there's an error in listing feature flags.
   */
  fun listFeatureFlags(limit: Int = 100, offset: Int = 0): List<FeatureFlagEntity> {
    return repository.list(limit, offset)
  }

  /**
   * Enable a feature flag.
   *
   * @param code The code of the feature flag to enable.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   * @throws FeatureFlagError If there's an error in enabling the feature flag.
   */
  fun enableFeatureFlag(code: String) {
    updateFeatureFlag(code, mapOf("enabled" to true))
  }

  /**
   * Disable a feature flag.
   *
   * @param code The code of the feature flag to disable.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   * @throws FeatureFlagError If there's an error in disabling the feature flag.
   */
  fun disableFeatureFlag(code: String) {
    updateFeatureFlag(code, mapOf("enabled" to false))
  }

  private fun parseMetadata(metadata: Any?): String {
    return when (metadata) {
      is Map<*, *> -> metadata.entries.joinToString(",") { "${it.key}:${it.value}" }
      else -> ""
    }
  }
}