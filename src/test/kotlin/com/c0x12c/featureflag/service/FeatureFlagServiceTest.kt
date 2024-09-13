package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlagCache
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class FeatureFlagCacheServiceUnitTest {

    private lateinit var repository: FeatureFlagRepository
    private lateinit var cache: RedisCache
    private lateinit var service: FeatureFlagService

    @BeforeEach
    fun setUp() {
        repository = mockk()
        cache = mockk()
        service = FeatureFlagService(repository, cache)
    }

    @Test
    fun `createFeatureFlag should insert flag and cache it`() = runBlocking {
        // Arrange
        val flagData = mapOf("name" to "Test Feature", "code" to "test_feature", "enabled" to true)
        val featureFlagCache = FeatureFlagCache(
            id = UUID.randomUUID(),
            name = "Test Feature",
            code = "test_feature",
            enabled = true
        )

        coEvery { repository.insert(any()) } returns featureFlagCache.id
        coEvery { repository.getById(featureFlagCache.id) } returns featureFlagCache
        coEvery { cache.set(any(), any()) } returns Unit

        // Act
        val result = service.createFeatureFlag(flagData)

        // Assert
        assertEquals(featureFlagCache, result)
        coVerify { repository.insert(any()) }
        coVerify { cache.set(featureFlagCache.code, featureFlagCache) }
    }

    @Test
    fun `getFeatureFlagByCode should return flag from cache if present`() = runBlocking {
        // Arrange
        val featureFlagCache = FeatureFlagCache(
            id = UUID.randomUUID(),
            name = "Test Feature",
            code = "test_feature",
            enabled = true
        )

        coEvery { cache.get(featureFlagCache.code) } returns featureFlagCache

        // Act
        val result = service.getFeatureFlagByCode(featureFlagCache.code)

        // Assert
        assertEquals(featureFlagCache, result)
        coVerify(exactly = 1) { cache.get(featureFlagCache.code) }
        coVerify(exactly = 0) { repository.getByCode(any()) }  // Verify repository is not called
    }

    @Test
    fun `getFeatureFlagByCode should fetch from repository if cache is empty`() = runBlocking {
        // Arrange
        val featureFlagCache = FeatureFlagCache(
            id = UUID.randomUUID(),
            name = "Test Feature",
            code = "test_feature",
            enabled = true
        )

        coEvery { cache.get(featureFlagCache.code) } returns null
        coEvery { repository.getByCode(featureFlagCache.code) } returns featureFlagCache
        coEvery { cache.set(any(), any()) } returns Unit

        // Act
        val result = service.getFeatureFlagByCode(featureFlagCache.code)

        // Assert
        assertEquals(featureFlagCache, result)
        coVerify { cache.get(featureFlagCache.code) }
        coVerify { repository.getByCode(featureFlagCache.code) }
        coVerify { cache.set(featureFlagCache.code, featureFlagCache) }
    }

    @Test
    fun `updateFeatureFlag should update repository and cache`() = runBlocking {
        // Arrange
        val existingFlag = FeatureFlagCache(
            id = UUID.randomUUID(),
            name = "Old Feature",
            code = "test_feature",
            enabled = false
        )
        val updatedFlagData = mapOf("name" to "Updated Feature", "enabled" to true)

        coEvery { repository.getByCode(existingFlag.code) } returns existingFlag
        coEvery { repository.update(any()) } returns Unit
        coEvery { cache.set(any(), any()) } returns Unit

        // Act
        service.updateFeatureFlag(existingFlag.code, updatedFlagData)

        // Assert
        coVerify { repository.update(any()) }
        coVerify { cache.set(existingFlag.code, any()) }
    }

    @Test
    fun `deleteFeatureFlag should remove from repository and cache`() = runBlocking {
        // Arrange
        val featureFlagCache = FeatureFlagCache(
            id = UUID.randomUUID(),
            name = "Test Feature",
            code = "test_feature",
            enabled = true
        )

        coEvery { repository.getByCode(featureFlagCache.code) } returns featureFlagCache
        coEvery { repository.delete(featureFlagCache.id) } returns Unit
        coEvery { cache.delete(featureFlagCache.code) } returns Unit

        // Act
        service.deleteFeatureFlag(featureFlagCache.code)

        // Assert
        coVerify { repository.delete(featureFlagCache.id) }
        coVerify { cache.delete(featureFlagCache.code) }
    }
}
