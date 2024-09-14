package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlagCache
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import java.time.Instant
import java.util.UUID

class FeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val cacheTTLSeconds: Long = 3600
) {

  /**
   * Create a new feature flag.
   *
   * @param flagData Map containing the feature flag data.
   * @return The created feature flag as a Map.
   * @throws FeatureFlagError If there's an error in creating the feature flag.
   */
  fun createFeatureFlag(flagData: Map<String, Any>): Map<String, Any> {
    val createdFlagId = repository.insert(flagData)
    val newFlag = repository.getById(createdFlagId) ?: throw FeatureFlagError("Failed to retrieve created flag")
    cache?.set(newFlag["code"] as String, newFlag.toFeatureFlagCache(), cacheTTLSeconds)
    return newFlag
  }

  /**
   * Retrieve a feature flag by its code.
   *
   * @param code The code of the feature flag.
   * @return The feature flag as a Map if found.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun getFeatureFlagByCode(code: String): Map<String, Any> {
    cache?.get(code)?.let {
      return it.toMap()
    }

    val featureFlag =
      repository.getByCode(code) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, featureFlag.toFeatureFlagCache(), cacheTTLSeconds)
    return featureFlag
  }

  /**
   * Update a feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param flagData Map containing the updated feature flag data.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   * @throws FeatureFlagError If there's an error in updating the feature flag.
   */
  fun updateFeatureFlag(code: String, flagData: Map<String, Any>): Boolean {
    // Attempt to update the feature flag in the repository
    val updateResult = repository.update(code, flagData)

    // If updateResult is not null, update the cache
    if (updateResult != null) {
      cache?.set(code, updateResult.toFeatureFlagCache(), cacheTTLSeconds)
      return true
    }

    // Return false if the update failed (updateResult was null)
    return false
  }

  /**
   * Delete a feature flag (soft delete).
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
   * List feature flags with optional pagination.
   *
   * @param limit The maximum number of feature flags to return.
   * @param offset The number of feature flags to skip.
   * @return A list of feature flags as Maps.
   */
  fun listFeatureFlags(limit: Int = 100, offset: Int = 0): List<Map<String, Any>> {
    return repository.list(limit, offset)
  }

  /**
   * Enable a feature flag.
   *
   * @param code The code of the feature flag to enable.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   * @throws FeatureFlagError If there's an error in enabling the feature flag.
   */
  fun enableFeatureFlag(code: String): Boolean {
    return updateFeatureFlag(code, mapOf("enabled" to true))
  }

  /**
   * Disable a feature flag.
   *
   * @param code The code of the feature flag to disable.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   * @throws FeatureFlagError If there's an error in disabling the feature flag.
   */
  fun disableFeatureFlag(code: String): Boolean {
    return updateFeatureFlag(code, mapOf("enabled" to false))
  }

  private fun Map<String, Any>.toFeatureFlagCache(): FeatureFlagCache {
    return FeatureFlagCache(
      id = this["id"] as UUID,
      name = this["name"] as String,
      code = this["code"] as String,
      description = this["description"] as? String ?: "",
      enabled = this["enabled"] as Boolean,
      metadata = parseMetadata(this["metadata"]),
      createdAt = Instant.parse(this["createdAt"] as String),
      updatedAt = (this["updatedAt"] as? String)?.takeIf { it.isNotBlank() }?.let { Instant.parse(it) },
      deletedAt = (this["deletedAt"] as? String)?.takeIf { it.isNotBlank() }?.let { Instant.parse(it) }
    )
  }

  private fun parseMetadata(metadata: Any?): Map<String, String> {
    return when (metadata) {
      is Map<*, *> -> metadata.mapKeys { it.key.toString() }
        .mapValues { it.value?.toString() ?: "" }

      is String -> metadata.split(",")
        .map { it.split(":") }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }

      else -> emptyMap()
    }
  }

  private fun FeatureFlagCache.toMap(): Map<String, Any> {
    return mapOf(
      "id" to id,
      "name" to name,
      "code" to code,
      "description" to (description ?: ""),
      "enabled" to enabled,
      "metadata" to metadata,
      "createdAt" to createdAt.toString(),
      "updatedAt" to (updatedAt?.toString() ?: ""),
      "deletedAt" to (deletedAt?.toString() ?: "")
    )
  }
}
