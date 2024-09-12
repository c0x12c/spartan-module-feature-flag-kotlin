package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import java.util.UUID

class FeatureFlagService(
    private val repository: FeatureFlagRepository,
    private val cache: RedisCache? = null
) {

    // Create a new feature flag and cache it if caching is enabled
    suspend fun createFeatureFlag(flagData: Map<String, Any>): FeatureFlag {
        // Convert flagData to FeatureFlag entity
        val featureFlag = FeatureFlag(
            id = UUID.randomUUID(),
            name = flagData["name"] as String,
            code = flagData["code"] as String,
            description = flagData["description"] as? String,
            enabled = flagData["enabled"] as? Boolean ?: false,
            metadata = flagData["metadata"] as? Map<String, Any>
        )

        // Insert the feature flag into the repository (database)
        repository.insert(featureFlag)

        // Cache the feature flag if caching is enabled
        cache?.set(featureFlag.code, featureFlag)

        return featureFlag
    }

    // Retrieve a feature flag by its code, checking the cache first
    suspend fun getFeatureFlagByCode(code: String): FeatureFlag? {
        // Try to retrieve from cache if cache is enabled
        cache?.get(code)?.let {
            return it
        }

        // Retrieve from repository (database)
        val featureFlag = repository.getByCode(code)

        // If found, cache the feature flag
        if (featureFlag != null && cache != null) {
            cache.set(code, featureFlag)
        }

        return featureFlag
    }

    // List feature flags with optional pagination
    suspend fun listFeatureFlags(limit: Int = 100, skip: Int = 0): List<FeatureFlag> {
        // List feature flags from the repository
        return repository.list(limit, skip)
    }

    // Update a feature flag's fields and update the cache
    suspend fun updateFeatureFlag(code: String, flagData: Map<String, Any>) {
        // Retrieve the existing feature flag by its code
        val existingFlag = getFeatureFlagByCode(code) ?: throw Exception("Feature flag not found.")

        // Update the existing feature flag with new values
        val updatedFlag = existingFlag.copy(
            name = flagData["name"] as? String ?: existingFlag.name,
            description = flagData["description"] as? String ?: existingFlag.description,
            enabled = flagData["enabled"] as? Boolean ?: existingFlag.enabled,
            metadata = flagData["metadata"] as? Map<String, Any> ?: existingFlag.metadata
        )

        // Update the repository (database)
        repository.update(updatedFlag)

        // Update the cache if caching is enabled
        cache?.set(updatedFlag.code, updatedFlag)
    }

    // Enable a feature flag by its code
    suspend fun enableFeatureFlag(code: String) {
        updateFeatureFlag(code, mapOf("enabled" to true))
    }

    // Disable a feature flag by its code
    suspend fun disableFeatureFlag(code: String) {
        updateFeatureFlag(code, mapOf("enabled" to false))
    }

    // Delete a feature flag by its code and remove it from the cache
    suspend fun deleteFeatureFlag(code: String) {
        // Retrieve the feature flag from the repository
        val featureFlag = repository.getByCode(code) ?: throw Exception("Feature flag not found.")

        // Delete the feature flag from the repository (database)
        repository.delete(featureFlag.id)

        // Remove the feature flag from the cache if caching is enabled
        cache?.delete(code)
    }
}
