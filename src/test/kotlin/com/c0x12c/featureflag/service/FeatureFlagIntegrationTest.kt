package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.redisCache
import com.c0x12c.featureflag.service.utils.TestUtils.service
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeatureFlagIntegrationTest {

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      TestUtils.setup()
    }
  }

  @BeforeEach
  fun clearData() {
    TestUtils.clearData()
    redisCache.clearAll()
  }

  @Test
  fun `create and retrieve feature flag`() {
    val featureFlag = FeatureFlag(
      name = "Test Flag",
      code = "TEST_FLAG",
      description = "A test flag",
      enabled = true,
      metadata = JsonObject(mapOf("key" to JsonPrimitive("value"))).toString()
    )

    val createdFlag = service.createFeatureFlag(featureFlag)

    assertNotNull(createdFlag)
    assertEquals("Test Flag", createdFlag.name)
    assertEquals("TEST_FLAG", createdFlag.code)

    val retrievedFlag = service.getFeatureFlagByCode("TEST_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Test Flag", retrievedFlag.name)
    assertEquals("TEST_FLAG", retrievedFlag.code)
    assertEquals(true, retrievedFlag.enabled)
    assertEquals(JsonObject(mapOf("key" to JsonPrimitive("value"))), service.getMetadataAsJsonObject(retrievedFlag))
  }

  @Test
  fun `update feature flag`() {
    val originalFlag = FeatureFlag(
      name = "Original Flag",
      code = "UPDATE_FLAG",
      enabled = false
    )

    service.createFeatureFlag(originalFlag)

    val updatedFlag = originalFlag.copy(
      name = "Updated Flag",
      enabled = true
    )

    service.updateFeatureFlag("UPDATE_FLAG", updatedFlag)

    val retrievedFlag = service.getFeatureFlagByCode("UPDATE_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Updated Flag", retrievedFlag.name)
    assertEquals(true, retrievedFlag.enabled)
  }

  @Test
  fun `delete feature flag`() {
    val featureFlag = FeatureFlag(
      name = "To Be Deleted",
      code = "DELETE_FLAG",
      enabled = true
    )

    service.createFeatureFlag(featureFlag)
    service.deleteFeatureFlag("DELETE_FLAG")

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode("DELETE_FLAG")
    }
  }

  @Test
  fun `list feature flags`() {
    val flag1 = FeatureFlag(
      name = "Flag 1",
      code = "FLAG_1",
      enabled = true
    )
    val flag2 = FeatureFlag(
      name = "Flag 2",
      code = "FLAG_2",
      enabled = false
    )

    service.createFeatureFlag(flag1)
    service.createFeatureFlag(flag2)

    val flags = service.listFeatureFlags()

    assertEquals(2, flags.size)
    assertEquals("Flag 1", flags[0].name)
    assertEquals("Flag 2", flags[1].name)
  }

  @Test
  fun `enable and disable feature flag`() {
    val featureFlag = FeatureFlag(
      name = "Toggle Flag",
      code = "TOGGLE_FLAG",
      enabled = false
    )

    service.createFeatureFlag(featureFlag)

    service.enableFeatureFlag("TOGGLE_FLAG")
    var flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(true, flag.enabled)

    service.disableFeatureFlag("TOGGLE_FLAG")
    flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(false, flag.enabled)
  }
}
