package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.redisCache
import com.c0x12c.featureflag.service.utils.TestUtils.service
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
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
    val featureFlag = FeatureFlag(
      name = "Test Flag",
      code = "TEST_FLAG",
      description = "A test flag",
      enabled = true,
      metadata = MetadataContent.UserTargeting(listOf("user1"), 50.0)
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
    assertTrue(retrievedFlag.metadata is MetadataContent.UserTargeting)
  }

  @Test
  fun `update feature flag`() {
    val originalFlag = FeatureFlag(
      name = "Original Flag",
      code = "UPDATE_FLAG",
      enabled = false,
      metadata = MetadataContent.UserTargeting(listOf("user1"), 50.0)
    )

    service.createFeatureFlag(originalFlag)

    val updatedFlag = originalFlag.copy(
      name = "Updated Flag",
      enabled = true,
      metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0)
    )

    service.updateFeatureFlag("UPDATE_FLAG", updatedFlag)

    val retrievedFlag = service.getFeatureFlagByCode("UPDATE_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Updated Flag", retrievedFlag.name)
    assertEquals(true, retrievedFlag.enabled)
    assertTrue(retrievedFlag.metadata is MetadataContent.GroupTargeting)
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
      enabled = true,
      metadata = MetadataContent.UserTargeting(listOf("user1"), 50.0)
    )
    val flag2 = FeatureFlag(
      name = "Flag 2",
      code = "FLAG_2",
      enabled = false,
      metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0)
    )

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
    val userTargetedFlag = FeatureFlag(
      name = "User Targeted Flag",
      code = "USER_FLAG",
      enabled = true,
      metadata = MetadataContent.UserTargeting(listOf("user1", "user2"), 73.0) // 70% gradual rollout
    )
    service.createFeatureFlag(userTargetedFlag)

    // user1 should be targeted because it is in the list and based on percentage
    assertTrue(service.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user1")))

    // user3 is not in the user list, so it should not be targeted
    assertFalse(service.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user3")))

    // GroupTargeting test case with percentage-based logic
    val groupTargetedFlag = FeatureFlag(
      name = "Group Targeted Flag",
      code = "GROUP_FLAG",
      enabled = true,
      metadata = MetadataContent.GroupTargeting(listOf("group1", "group2"), 70.0) // 70% gradual rollout
    )
    service.createFeatureFlag(groupTargetedFlag)

    // group1 should be targeted because it is in the list and based on percentage
    assertTrue(service.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group1")))

    // group3 is not in the group list, so it should not be targeted
    assertFalse(service.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group3")))

    // TimeBasedActivation test case
    val timeBasedFlag = FeatureFlag(
      name = "Time Based Flag",
      code = "TIME_FLAG",
      enabled = true,
      metadata = MetadataContent.TimeBasedActivation(
        startTime = Instant.now().minusSeconds(3600), // Started an hour ago
        endTime = Instant.now().plusSeconds(3600) // Ends in an hour
      )
    )
    service.createFeatureFlag(timeBasedFlag)

    // Time-based flag should be enabled since current time is within the activation window
    assertTrue(service.isFeatureFlagEnabled("TIME_FLAG", emptyMap()))
  }

  @Test
  fun `find feature flags by metadata type`() {
    val userFlag1 = FeatureFlag(
      name = "User Flag 1",
      code = "USER_FLAG_1",
      enabled = true,
      metadata = MetadataContent.UserTargeting(listOf("user1"), 50.0)
    )
    val userFlag2 = FeatureFlag(
      name = "User Flag 2",
      code = "USER_FLAG_2",
      enabled = true,
      metadata = MetadataContent.UserTargeting(listOf("user2"), 60.0)
    )
    val groupFlag = FeatureFlag(
      name = "Group Flag",
      code = "GROUP_FLAG",
      enabled = true,
      metadata = MetadataContent.GroupTargeting(listOf("group1"), 70.0)
    )

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
    val gradualRolloutFlag = FeatureFlag(
      name = "Gradual Rollout Flag",
      code = "GRADUAL_FLAG",
      enabled = true,
      metadata = MetadataContent.GradualRollout(
        startPercentage = 0.0,
        endPercentage = 100.0,
        startTime = Instant.now().minusSeconds(3600),
        duration = java.time.Duration.ofHours(2)
      )
    )
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
}
