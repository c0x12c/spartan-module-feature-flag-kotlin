package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.redisCache
import com.c0x12c.featureflag.service.utils.TestUtils.service
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
    val featureFlag = FeatureFlag(name = "Test Flag", code = "TEST_FLAG", description = "A test flag", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))

    val createdFlag = service.createFeatureFlag(featureFlag)

    assertNotNull(createdFlag)
    assertEquals("Test Flag", createdFlag.name)
    assertEquals("TEST_FLAG", createdFlag.code)

    val retrievedFlag = service.getFeatureFlagByCode("TEST_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Test Flag", retrievedFlag.name)
    assertEquals("TEST_FLAG", retrievedFlag.code)
    assertEquals(true, retrievedFlag.enabled)
    assertTrue(retrievedFlag.metadata is MetadataContent.UserTargeting)
  }

  @Test
  fun `update feature flag`() {
    val originalFlag = FeatureFlag(name = "Original Flag", code = "UPDATE_FLAG", enabled = false, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))

    service.createFeatureFlag(originalFlag)

    val updatedFlag = originalFlag.copy(name = "Updated Flag", enabled = true, metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0))

    service.updateFeatureFlag("UPDATE_FLAG", updatedFlag)

    val retrievedFlag = service.getFeatureFlagByCode("UPDATE_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Updated Flag", retrievedFlag.name)
    assertEquals(true, retrievedFlag.enabled)
    assertTrue(retrievedFlag.metadata is MetadataContent.GroupTargeting)
  }

  @Test
  fun `delete feature flag`() {
    val featureFlag = FeatureFlag(name = "To Be Deleted", code = "DELETE_FLAG", enabled = true)

    service.createFeatureFlag(featureFlag)
    service.deleteFeatureFlag("DELETE_FLAG")

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode("DELETE_FLAG")
    }
  }

  @Test
  fun `list feature flags`() {
    val flag1 = FeatureFlag(name = "Flag 1", code = "FLAG_1", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))
    val flag2 = FeatureFlag(name = "Flag 2", code = "FLAG_2", enabled = false, metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0))

    service.createFeatureFlag(flag1)
    service.createFeatureFlag(flag2)

    val flags = service.listFeatureFlags()

    assertEquals(2, flags.size)
    assertEquals("Flag 1", flags[0].name)
    assertEquals("Flag 2", flags[1].name)
  }

  @Test
  fun `test isFeatureFlagEnabled with different metadata types`() {
    // UserTargeting test case with percentage-based logic
    val userTargetedFlag =
      FeatureFlag(
        name = "User Targeted Flag",
        code = "USER_FLAG",
        enabled = true,
        metadata =
          MetadataContent.UserTargeting(
            targetedUserIds = listOf("user1", "user2"),
            // 70% gradual rollout
            percentage = 73.0
          )
      )
    service.createFeatureFlag(userTargetedFlag)

    // user1 should be targeted because it is in the list and based on percentage
    assertTrue(service.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user1")))

    // user3 is not in the user list, so it should not be targeted
    assertFalse(service.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user3")))

    // GroupTargeting test case with percentage-based logic
    val groupTargetedFlag =
      FeatureFlag(
        name = "Group Targeted Flag",
        code = "GROUP_FLAG",
        enabled = true,
        metadata =
          MetadataContent.GroupTargeting(
            groupIds = listOf("group1", "group2"),
            // 70% gradual rollout
            percentage = 70.0
          )
      )
    service.createFeatureFlag(groupTargetedFlag)

    // group1 should be targeted because it is in the list and based on percentage
    assertTrue(service.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group1")))

    // group3 is not in the group list, so it should not be targeted
    assertFalse(service.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group3")))

    // TimeBasedActivation test case
    val timeBasedFlag =
      FeatureFlag(
        name = "Time Based Flag",
        code = "TIME_FLAG",
        enabled = true,
        metadata =
          MetadataContent.TimeBasedActivation(
            startTime = Instant.now().minusSeconds(3600),
            endTime = Instant.now().plusSeconds(3600)
          )
      )
    service.createFeatureFlag(timeBasedFlag)

    // Time-based flag should be enabled since current time is within the activation window
    assertTrue(service.isFeatureFlagEnabled("TIME_FLAG", emptyMap()))
  }

  @Test
  fun `find feature flags by metadata type`() {
    val userFlag1 = FeatureFlag(name = "User Flag 1", code = "USER_FLAG_1", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))
    val userFlag2 = FeatureFlag(name = "User Flag 2", code = "USER_FLAG_2", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user2"), percentage = 60.0))
    val groupFlag = FeatureFlag(name = "Group Flag", code = "GROUP_FLAG", enabled = true, metadata = MetadataContent.GroupTargeting(listOf("group1"), 70.0))

    service.createFeatureFlag(userFlag1)
    service.createFeatureFlag(userFlag2)
    service.createFeatureFlag(groupFlag)

    val userFlags = service.findFeatureFlagsByMetadataType("UserTargeting")
    assertEquals(2, userFlags.size)
    assertTrue(userFlags.all { it.metadata is MetadataContent.UserTargeting })

    val groupFlags = service.findFeatureFlagsByMetadataType("GroupTargeting")
    assertEquals(1, groupFlags.size)
    assertTrue(groupFlags.all { it.metadata is MetadataContent.GroupTargeting })
  }

  @Test
  fun `test gradual rollout`() {
    val gradualRolloutFlag = FeatureFlag(name = "Gradual Rollout Flag", code = "GRADUAL_FLAG", enabled = true, metadata = MetadataContent.GradualRollout(startPercentage = 0.0, endPercentage = 100.0, startTime = Instant.now().minusSeconds(3600), duration = java.time.Duration.ofHours(2)))
    service.createFeatureFlag(gradualRolloutFlag)

    // This test is probabilistic and may occasionally fail
    var enabledCount = 0
    for (i in 1..1000) {
      if (service.isFeatureFlagEnabled("GRADUAL_FLAG", mapOf("userId" to "user$i"))) {
        enabledCount++
      }
    }

    // After 1 hour (half of the duration), we expect roughly 50% of users to have the flag enabled
    assertTrue(enabledCount in 400..600)
  }

  @Test
  fun `getMetadataValue should return null for feature flag with null metadata`() {
    val flag = createFeatureFlag("Null Metadata", "NULL_METADATA", metadata = null)
    service.createFeatureFlag(flag)
    assertNull(service.getMetadataValue("NULL_METADATA", "anyKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for UserTargeting`() {
    val metadata = MetadataContent.UserTargeting(whitelistedUsers = mapOf("user1" to true, "user2" to false), blacklistedUsers = mapOf("user3" to true), targetedUserIds = listOf("user4", "user5"), percentage = 50.0, defaultValue = true)
    val flag = createFeatureFlag("User Targeting", "USER_TARGETING", metadata = metadata)
    service.createFeatureFlag(flag)

    assertEquals("user1:true,user2:false", service.getMetadataValue("USER_TARGETING", "whitelistedUsers"))
    assertEquals("user3:true", service.getMetadataValue("USER_TARGETING", "blacklistedUsers"))
    assertEquals("user4,user5", service.getMetadataValue("USER_TARGETING", "targetedUserIds"))
    assertEquals("50.0", service.getMetadataValue("USER_TARGETING", "percentage"))
    assertEquals("true", service.getMetadataValue("USER_TARGETING", "defaultValue"))
    assertNull(service.getMetadataValue("USER_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GroupTargeting`() {
    val metadata = MetadataContent.GroupTargeting(listOf("group1", "group2"), 75.0)
    val flag = createFeatureFlag("Group Targeting", "GROUP_TARGETING", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("group1,group2", service.getMetadataValue("GROUP_TARGETING", "groupIds"))
    Assertions.assertEquals("75.0", service.getMetadataValue("GROUP_TARGETING", "percentage"))
    assertNull(service.getMetadataValue("GROUP_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for TimeBasedActivation`() {
    val startTime = Instant.parse("2023-01-01T00:00:00Z")
    val endTime = Instant.parse("2023-12-31T23:59:59Z")
    val metadata = MetadataContent.TimeBasedActivation(startTime, endTime)
    val flag = createFeatureFlag("Time Based", "TIME_BASED", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals(startTime.toString(), service.getMetadataValue("TIME_BASED", "startTime"))
    Assertions.assertEquals(endTime.toString(), service.getMetadataValue("TIME_BASED", "endTime"))
    assertNull(service.getMetadataValue("TIME_BASED", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GradualRollout`() {
    val startTime = Instant.parse("2023-01-01T00:00:00Z")
    val duration = Duration.ofDays(30)
    val metadata = MetadataContent.GradualRollout(0.0, 100.0, startTime, duration)
    val flag = createFeatureFlag("Gradual Rollout", "GRADUAL_ROLLOUT", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("0.0", service.getMetadataValue("GRADUAL_ROLLOUT", "startPercentage"))
    Assertions.assertEquals("100.0", service.getMetadataValue("GRADUAL_ROLLOUT", "endPercentage"))
    Assertions.assertEquals(startTime.toString(), service.getMetadataValue("GRADUAL_ROLLOUT", "startTime"))
    Assertions.assertEquals(duration.toString(), service.getMetadataValue("GRADUAL_ROLLOUT", "duration"))
    assertNull(service.getMetadataValue("GRADUAL_ROLLOUT", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for ABTestingConfig`() {
    val metadata = MetadataContent.ABTestingConfig("A", "B", 60.0)
    val flag = createFeatureFlag("AB Testing", "AB_TESTING", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("A", service.getMetadataValue("AB_TESTING", "variantA"))
    Assertions.assertEquals("B", service.getMetadataValue("AB_TESTING", "variantB"))
    Assertions.assertEquals("60.0", service.getMetadataValue("AB_TESTING", "distribution"))
    assertNull(service.getMetadataValue("AB_TESTING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for VersionTargeting`() {
    val metadata = MetadataContent.VersionTargeting("1.0.0", "2.0.0")
    val flag = createFeatureFlag("Version Targeting", "VERSION_TARGETING", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("1.0.0", service.getMetadataValue("VERSION_TARGETING", "minVersion"))
    Assertions.assertEquals("2.0.0", service.getMetadataValue("VERSION_TARGETING", "maxVersion"))
    assertNull(service.getMetadataValue("VERSION_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GeographicTargeting`() {
    val metadata = MetadataContent.GeographicTargeting(listOf("US", "UK"), listOf("CA", "NY"))
    val flag = createFeatureFlag("Geographic Targeting", "GEO_TARGETING", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("US,UK", service.getMetadataValue("GEO_TARGETING", "countries"))
    Assertions.assertEquals("CA,NY", service.getMetadataValue("GEO_TARGETING", "regions"))
    assertNull(service.getMetadataValue("GEO_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for DeviceTargeting`() {
    val metadata = MetadataContent.DeviceTargeting(listOf("iOS", "Android"), listOf("Mobile", "Tablet"))
    val flag = createFeatureFlag("Device Targeting", "DEVICE_TARGETING", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("iOS,Android", service.getMetadataValue("DEVICE_TARGETING", "platforms"))
    Assertions.assertEquals("Mobile,Tablet", service.getMetadataValue("DEVICE_TARGETING", "deviceTypes"))
    assertNull(service.getMetadataValue("DEVICE_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for CustomRules`() {
    val metadata = MetadataContent.CustomRules(mapOf("rule1" to "value1", "rule2" to "value2"))
    val flag = createFeatureFlag("Custom Rules", "CUSTOM_RULES", metadata = metadata)
    service.createFeatureFlag(flag)

    Assertions.assertEquals("value1", service.getMetadataValue("CUSTOM_RULES", "rule1"))
    Assertions.assertEquals("value2", service.getMetadataValue("CUSTOM_RULES", "rule2"))
    assertNull(service.getMetadataValue("CUSTOM_RULES", "invalidKey"))
  }

  @Test
  fun `enable and disable feature flag`() {
    val featureFlag = FeatureFlag(name = "Test Flag", code = "TOGGLE_FLAG", description = "A test flag for toggling", enabled = false)

    val createdFlag = service.createFeatureFlag(featureFlag)
    assertFalse(createdFlag.enabled)

    val enabledFlag = service.enableFeatureFlag("TOGGLE_FLAG")
    assertTrue(enabledFlag.enabled)

    val disabledFlag = service.disableFeatureFlag("TOGGLE_FLAG")
    assertFalse(disabledFlag.enabled)

    assertThrows<FeatureFlagNotFoundError> {
      service.enableFeatureFlag("NONEXISTENT_FLAG")
    }

    assertThrows<FeatureFlagNotFoundError> {
      service.disableFeatureFlag("NONEXISTENT_FLAG")
    }
  }

  @Test
  fun `isFeatureFlagEnabled should respect enabled status`() {
    val featureFlag = FeatureFlag(name = "Always Off Flag", code = "ALWAYS_OFF", description = "A flag that's always off", enabled = false, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 100.0))

    service.createFeatureFlag(featureFlag)

    assertFalse(service.isFeatureFlagEnabled("ALWAYS_OFF", mapOf("userId" to "user1")))

    service.enableFeatureFlag("ALWAYS_OFF")

    assertTrue(service.isFeatureFlagEnabled("ALWAYS_OFF", mapOf("userId" to "user1")))
  }

  private fun createFeatureFlag(
    name: String,
    code: String,
    description: String? = null,
    enabled: Boolean = true,
    metadata: MetadataContent? = null
  ): FeatureFlag = FeatureFlag(id = UUID.randomUUID(), name = name, code = code, description = description, enabled = enabled, metadata = metadata, createdAt = Instant.now())
}
