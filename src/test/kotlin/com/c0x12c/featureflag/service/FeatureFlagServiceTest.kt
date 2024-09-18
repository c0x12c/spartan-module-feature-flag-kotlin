package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
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
    val featureFlag = FeatureFlag(
      name = "Test Flag",
      code = "TEST_FLAG",
      description = "A test flag",
      enabled = true,
      metadata = "{\"key\":\"value\"}"
    )

    val createdFlagId = UUID.randomUUID()
    val createdFlag = featureFlag.copy(id = createdFlagId, createdAt = Instant.parse("2023-09-13T12:00:00Z"))

    every { repository.insert(any()) } returns createdFlagId
    every { repository.getById(createdFlagId) } returns createdFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.createFeatureFlag(featureFlag)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals("TEST_FLAG", result.code)
    assertEquals(true, result.enabled)

    verify { repository.insert(featureFlag) }
    verify { repository.getById(createdFlagId) }
    verify { cache.set("TEST_FLAG", any(), 3600L) }
  }

  @Test
  fun `getFeatureFlagByCode should return flag from cache if available`() {
    val code = "TEST_FLAG"
    val cachedFlag = FeatureFlag(
      id = UUID.randomUUID(),
      name = "Test Flag",
      code = code,
      description = "A test flag",
      enabled = true,
      metadata = "{\"key\":\"value\"}",
      createdAt = Instant.parse("2023-09-13T12:00:00Z")
    )

    every { cache.get(code) } returns cachedFlag

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals(code, result.code)
    assertEquals(true, result.enabled)

    verify { cache.get(code) }
    verify(exactly = 0) { repository.getByCode(code) }
  }

  @Test
  fun `getFeatureFlagByCode should fetch from repository if not in cache`() {
    val code = "TEST_FLAG"
    val repoFlag = FeatureFlag(
      id = UUID.randomUUID(),
      name = "Test Flag",
      code = code,
      enabled = true,
      createdAt = Instant.parse("2023-09-13T12:00:00Z")
    )

    every { cache.get(code) } returns null
    every { repository.getByCode(code) } returns repoFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.getFeatureFlagByCode(code)

    assertNotNull(result)
    assertEquals("Test Flag", result.name)
    assertEquals(code, result.code)
    assertEquals(true, result.enabled)

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
    val updatedFlag = FeatureFlag(
      id = UUID.randomUUID(),
      name = "New Name",
      code = code,
      enabled = true,
      createdAt = Instant.parse("2023-09-13T12:00:00Z"),
      updatedAt = Instant.parse("2023-09-13T13:00:00Z")
    )

    every { repository.update(code, any()) } returns updatedFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.updateFeatureFlag(code, updatedFlag)

    assertNotNull(result)
    assertEquals("New Name", result.name)
    assertEquals(true, result.enabled)

    verify { repository.update(code, updatedFlag) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `updateFeatureFlag should throw FeatureFlagNotFoundError if flag not found`() {
    val code = "NONEXISTENT_FLAG"
    val updateData = FeatureFlag(name = "New Name", code = code)

    every { repository.update(code, any()) } returns null

    assertThrows<FeatureFlagNotFoundError> {
      service.updateFeatureFlag(code, updateData)
    }

    verify { repository.update(code, updateData) }
  }

  @Test
  fun `deleteFeatureFlag should delete the flag`() {
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
      FeatureFlag(id = UUID.randomUUID(), name = "Flag 1", code = "FLAG_1"),
      FeatureFlag(id = UUID.randomUUID(), name = "Flag 2", code = "FLAG_2")
    )

    every { repository.list(100, 0) } returns flags

    val result = service.listFeatureFlags()

    assertEquals(2, result.size)
    assertEquals("Flag 1", result[0].name)
    assertEquals("FLAG_2", result[1].code)

    verify { repository.list(100, 0) }
  }

  @Test
  fun `enableFeatureFlag should enable the flag`() {
    val code = "TEST_FLAG"
    val enabledFlag = FeatureFlag(
      id = UUID.randomUUID(),
      name = "Test Flag",
      code = code,
      enabled = true,
      createdAt = Instant.parse("2023-09-13T12:00:00Z"),
      updatedAt = Instant.parse("2023-09-13T13:00:00Z")
    )

    every { cache.get(code) } returns null
    every { repository.getByCode(code) } returns enabledFlag.copy(enabled = false)
    every { repository.update(code, any()) } returns enabledFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.enableFeatureFlag(code)

    assertNotNull(result)
    assertEquals(true, result.enabled)

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
    verify { repository.update(code, any()) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `disableFeatureFlag should disable the flag`() {
    val code = "TEST_FLAG"
    val disabledFlag = FeatureFlag(
      id = UUID.randomUUID(),
      name = "Test Flag",
      code = code,
      enabled = false,
      createdAt = Instant.parse("2023-09-13T12:00:00Z"),
      updatedAt = Instant.parse("2023-09-13T13:00:00Z")
    )

    every { cache.get(code) } returns null
    every { repository.getByCode(code) } returns disabledFlag.copy(enabled = true)
    every { repository.update(code, any()) } returns disabledFlag
    every { cache.set(any(), any(), any()) } returns true

    val result = service.disableFeatureFlag(code)

    assertNotNull(result)
    assertEquals(false, result.enabled)

    verify { cache.get(code) }
    verify { repository.getByCode(code) }
    verify { repository.update(code, any()) }
    verify { cache.set(code, any(), 3600L) }
  }

  @Test
  fun `getMetadataAsJsonObject should return JsonObject`() {
    val featureFlag = FeatureFlag(
      name = "Test Flag",
      code = "TEST_FLAG",
      metadata = "{\"key\":\"value\"}"
    )

    val result = service.getMetadataAsJsonObject(featureFlag)

    assertNotNull(result)
    assertEquals("value", result["key"]?.toString()?.trim('"'))
  }
}
