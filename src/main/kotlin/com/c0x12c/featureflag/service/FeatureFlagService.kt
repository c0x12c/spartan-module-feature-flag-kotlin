package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagError
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class FeatureFlagService(
  private val repository: FeatureFlagRepository,
  private val cache: RedisCache? = null,
  private val cacheTTLSeconds: Long = 3600
) {

  /**
   * Create a new feature flag.
   *
   * @param featureFlag FeatureFlag object containing the feature flag data.
   * @return The created feature flag as a FeatureFlag object.
   * @throws FeatureFlagError If there's an error in creating the feature flag.
   */
  fun createFeatureFlag(featureFlag: FeatureFlag): FeatureFlag {
    val createdFlagId = repository.insert(featureFlag)
    val newFlag = repository.getById(createdFlagId) ?: throw FeatureFlagError("Failed to retrieve created flag")
    cache?.set(newFlag.code, newFlag, cacheTTLSeconds)
    return newFlag
  }

  /**
   * Retrieve a feature flag by its code.
   *
   * @param code The code of the feature flag.
   * @return The feature flag as a FeatureFlag object.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun getFeatureFlagByCode(code: String): FeatureFlag {
    cache?.get(code)?.let {
      return it
    }

    val featureFlag = repository.getByCode(code) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, featureFlag, cacheTTLSeconds)
    return featureFlag
  }

  /**
   * Update an existing feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param featureFlag FeatureFlag object containing the updated feature flag data.
   * @return The updated feature flag as a FeatureFlag object.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun updateFeatureFlag(code: String, featureFlag: FeatureFlag): FeatureFlag {
    val updatedFlag = repository.update(code, featureFlag) ?: throw FeatureFlagNotFoundError("Feature flag with code '$code' not found")
    cache?.set(code, updatedFlag, cacheTTLSeconds)
    return updatedFlag
  }

  /**
   * Delete a feature flag.
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
   * @return A list of FeatureFlag objects.
   */
  fun listFeatureFlags(limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return repository.list(limit, offset)
  }

  /**
   * Enable a feature flag.
   *
   * @param code The code of the feature flag to enable.
   * @return The updated FeatureFlag object with enabled set to true.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun enableFeatureFlag(code: String): FeatureFlag {
    val flag = getFeatureFlagByCode(code)
    return updateFeatureFlag(code, flag.copy(enabled = true))
  }

  /**
   * Disable a feature flag.
   *
   * @param code The code of the feature flag to disable.
   * @return The updated FeatureFlag object with enabled set to false.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun disableFeatureFlag(code: String): FeatureFlag {
    val flag = getFeatureFlagByCode(code)
    return updateFeatureFlag(code, flag.copy(enabled = false))
  }

  /**
   * Convert the metadata of a FeatureFlag from a JSON string to a JsonObject.
   *
   * @param featureFlag The FeatureFlag object containing the metadata.
   * @return The metadata as a JsonObject.
   */
  fun getMetadataAsJsonObject(featureFlag: FeatureFlag): JsonObject {
    return Json.parseToJsonElement(featureFlag.metadata).jsonObject
  }

  /**
   * Update the metadata of a feature flag.
   *
   * @param code The code of the feature flag to update.
   * @param newMetadata The new metadata as a JsonObject.
   * @return The updated FeatureFlag object.
   * @throws FeatureFlagNotFoundError If the feature flag is not found.
   */
  fun updateMetadata(code: String, newMetadata: JsonObject): FeatureFlag {
    val flag = getFeatureFlagByCode(code)
    val updatedMetadata = Json.encodeToString(JsonObject.serializer(), newMetadata)
    return updateFeatureFlag(code, flag.copy(metadata = updatedMetadata))
  }
}
