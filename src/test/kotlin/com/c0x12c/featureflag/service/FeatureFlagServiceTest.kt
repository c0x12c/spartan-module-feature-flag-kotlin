package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlagCache
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class FeatureFlagServiceTest {

  private lateinit var repository: FeatureFlagRepository
  private lateinit var cache: RedisCache
  private lateinit var service: FeatureFlagService

  @BeforeEach
  fun setup() {
    repository = mockk()
    cache = mockk()
    service = FeatureFlagService(repository, cache)
  }

  @Test
  fun `createFeatureFlag should create a new feature flag`() {
    val flagData = mapOf(
      "name" to "Test Flag",
      "code" to "TEST_FLAG",
      "description" to "A test flag",
      "enabled" to true,
      "metadata" to mapOf("key" to "value")
    )

    val createdFlagId = UUID.randomUUID()
    val createdFlag = mapOf(
      "id" to createdFlagId,
      "name" to "Test Flag",
      "code" to "TEST_FLAG",
      "description" to "A test flag",
      "enabled" to true,
      "metadata" to mapOf("key" to "value"),
      "createdAt" to "2023-09-13T12:00:00Z",
      "updatedAt" to null,
      "deletedAt" to null
    ) as Map<String, Any>

    every { repository.insert(any()) } returns createdFlagId
    every { repository.getById(createdFlagId) } returns createdFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.createFeatureFlag(flagData)

    assertNotNull(result)
    assertEquals("Test Flag", result["name"])
    assertEquals("TEST_FLAG", result["code"])
    assertEquals(true, result["enabled"])

    verify { repository.insert(flagData) }
    verify { repository.getById(createdFlagId) }
    verify { cache.set("TEST_FLAG", any(), 3600L) }
  }

  @Test
  fun `getFeatureFlagByCode should return flag from cache if available`() {
    val code = "TEST_FLAG"
    val cachedFlag = FeatureFlagCache(
      id = UUID.randomUUID(),
      name = "Test Flag",
      code = code,
      description = "A test flag",
      enabled = true,
      metadata = mapOf("key" to "value"),
      createdAt = Instant.parse("2023-09-13T12:00:00Z"),
      updatedAt = null,
      deletedAt = null
    )

    every { cache.get(code) } returns cachedFlag

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result["name"])
    assertEquals(code, result["code"])
    assertEquals(true, result["enabled"])

    verify { cache.get(code) }
    verify(exactly = 0) { repository.getByCode(code) }
  }

  @Test
  fun `getFeatureFlagByCode should fetch from repository if not in cache`() {
    val code = "TEST_FLAG"
    val repoFlag = mapOf(
      "id" to UUID.randomUUID(),
      "name" to "Test Flag",
      "code" to code,
      "enabled" to true,
      "createdAt" to "2023-09-13T12:00:00Z"
    )

    every { cache.get(code) } returns null
    every { repository.getByCode(code) } returns repoFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result["name"])
    assertEquals(code, result["code"])
    assertEquals(true, result["enabled"])

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `getFeatureFlagByCode should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"

    every { cache.get(code) } returns null
    every { repository.getByCode(code) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode(code)
    }

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
  }

  @Test
  fun `updateFeatureFlag should update existing flag`() {
    val code = "TEST_FLAG"
    val updateData = mapOf(
      "name" to "New Name",
      "enabled" to true
    )

    every { repository.update(code, updateData) } returns mapOf(
      "id" to UUID.randomUUID(),
      "name" to "New Name",
      "code" to code,
      "enabled" to true,
      "createdAt" to "2023-09-13T12:00:00Z",
      "updatedAt" to "2023-09-13T13:00:00Z"
    )
    every { cache.set(any(), any(), any()) } returns true

    service.updateFeatureFlag(code, updateData)

    verify { repository.update(code, updateData) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `updateFeatureFlag should return null if flag not found`() {
    val code = "NONEXISTENT_FLAG"
    val updateData = mapOf("name" to "New Name")

    every { repository.update(code, updateData) } returns null

    assertFalse {
      service.updateFeatureFlag(code, updateData)
    }

    verify { repository.update(code, updateData) }
  }

  @Test
  fun `deleteFeatureFlag should soft delete the flag`() {
    val code = "TEST_FLAG"

    every { repository.delete(code) } returns true
    every { cache.delete(any()) } returns true

    service.deleteFeatureFlag(code)

    verify { repository.delete(code) }
    verify { cache.delete(code) }
  }

  @Test
  fun `deleteFeatureFlag should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"

    every { repository.delete(code) } returns false

    assertThrows<FeatureFlagNotFoundError> {
      service.deleteFeatureFlag(code)
    }

    verify { repository.delete(code) }
  }

  @Test
  fun `listFeatureFlags should return list of flags`() {
    val flags = listOf(
      mapOf("id" to UUID.randomUUID(), "name" to "Flag 1", "code" to "FLAG_1"),
      mapOf("id" to UUID.randomUUID(), "name" to "Flag 2", "code" to "FLAG_2")
    )

    every { repository.list(100, 0) } returns flags

    val result = service.listFeatureFlags()

    assertEquals(2, result.size)
    assertEquals("Flag 1", result[0]["name"])
    assertEquals("FLAG_2", result[1]["code"])

    verify { repository.list(100, 0) }
  }

  @Test
  fun `enableFeatureFlag should enable the flag`() {
    val code = "TEST_FLAG"

    every { repository.update(code, mapOf("enabled" to true)) } returns mapOf(
      "id" to UUID.randomUUID(),
      "name" to "Test Flag",
      "code" to code,
      "enabled" to true,
      "createdAt" to "2023-09-13T12:00:00Z",
      "updatedAt" to "2023-09-13T13:00:00Z"
    )
    every { cache.set(any(), any(), any()) } returns true

    service.enableFeatureFlag(code)

    verify { repository.update(code, mapOf("enabled" to true)) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `disableFeatureFlag should disable the flag`() {
    val code = "TEST_FLAG"

    every { repository.update(code, mapOf("enabled" to false)) } returns mapOf(
      "id" to UUID.randomUUID(),
      "name" to "Test Flag",
      "code" to code,
      "enabled" to false,
      "createdAt" to "2023-09-13T12:00:00Z",
      "updatedAt" to "2023-09-13T13:00:00Z"
    )
    every { cache.set(any(), any(), any()) } returns true

    service.disableFeatureFlag(code)

    verify { repository.update(code, mapOf("enabled" to false)) }
    verify { cache.set(code, any(), 3600L) }
  }
}
