package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.redisCache
import com.c0x12c.featureflag.service.utils.TestUtils.service
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
    val flagData = mapOf(
      "name" to "Test Flag",
      "code" to "TEST_FLAG",
      "description" to "A test flag",
      "enabled" to true,
      "metadata" to mapOf("key" to "value")
    )

    val createdFlag = service.createFeatureFlag(flagData)

    assertNotNull(createdFlag)
    assertEquals("Test Flag", createdFlag["name"])
    assertEquals("TEST_FLAG", createdFlag["code"])

    val retrievedFlag = service.getFeatureFlagByCode("TEST_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Test Flag", retrievedFlag["name"])
    assertEquals("TEST_FLAG", retrievedFlag["code"])
    assertEquals(true, retrievedFlag["enabled"])
    assertEquals(mapOf("key" to "value"), retrievedFlag["metadata"])
  }

  @Test
  fun `update feature flag`() {
    val flagData = mapOf(
      "name" to "Original Flag",
      "code" to "UPDATE_FLAG",
      "enabled" to false
    )

    service.createFeatureFlag(flagData)
    service.updateFeatureFlag(
      "UPDATE_FLAG",
      mapOf(
        "name" to "Updated Flag",
        "enabled" to true
      )
    )

    val updatedFlag = service.getFeatureFlagByCode("UPDATE_FLAG")

    assertNotNull(updatedFlag)
    assertEquals("Updated Flag", updatedFlag["name"])
    assertEquals(true, updatedFlag["enabled"])
  }

  @Test
  fun `delete feature flag`() {
    val flagData = mapOf(
      "name" to "To Be Deleted",
      "code" to "DELETE_FLAG",
      "enabled" to true
    )

    service.createFeatureFlag(flagData)
    service.deleteFeatureFlag("DELETE_FLAG")

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode("DELETE_FLAG")
    }
  }

  @Test
  fun `list feature flags`() {
    val flag1 = mapOf(
      "name" to "Flag 1",
      "code" to "FLAG_1",
      "enabled" to true
    )
    val flag2 = mapOf(
      "name" to "Flag 2",
      "code" to "FLAG_2",
      "enabled" to false
    )

    service.createFeatureFlag(flag1)
    service.createFeatureFlag(flag2)

    val flags = service.listFeatureFlags()

    assertEquals(2, flags.size)
    assertEquals("Flag 1", flags[0]["name"])
    assertEquals("Flag 2", flags[1]["name"])
  }

  @Test
  fun `enable and disable feature flag`() {
    val flagData = mapOf(
      "name" to "Toggle Flag",
      "code" to "TOGGLE_FLAG",
      "enabled" to false
    )

    service.createFeatureFlag(flagData)

    service.enableFeatureFlag("TOGGLE_FLAG")
    var flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(true, flag["enabled"])

    service.disableFeatureFlag("TOGGLE_FLAG")
    flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(false, flag["enabled"])
  }
}
