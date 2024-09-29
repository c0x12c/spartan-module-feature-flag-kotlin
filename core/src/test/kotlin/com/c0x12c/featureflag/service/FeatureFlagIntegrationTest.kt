package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.client.SlackClient
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.notification.SlackNotifierConfig
import com.c0x12c.featureflag.service.utils.RandomUtils
import com.c0x12c.featureflag.service.utils.TestUtils
import com.c0x12c.featureflag.service.utils.TestUtils.redisCache
import com.c0x12c.featureflag.service.utils.TestUtils.repository
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import retrofit2.Response

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureFlagIntegrationTest {
  private lateinit var mockSlackClient: SlackClient
  private lateinit var slackNotifier: SlackNotifier
  private lateinit var featureFlagService: FeatureFlagService

  companion object {
    @BeforeAll
    @JvmStatic
    fun setup() {
      TestUtils.setupDependencies()
    }

    @AfterAll
    @JvmStatic
    fun tearDown() {
      TestUtils.cleanDependencies()
    }
  }

  @BeforeEach
  fun beforeEach() {
    TestUtils.clearData()
    redisCache.clearAll()

    mockSlackClient = mockk(relaxed = true)
    coEvery {
      mockSlackClient.sendMessage(any(), any())
    } returns Response.success(Unit)

    slackNotifier =
      SlackNotifier(
        SlackNotifierConfig(
          webhookUrl = "xxx",
          requestHeaders = mapOf("clientId" to "test", "apiKey" to "1234")
        )
      ) { mockSlackClient }

    featureFlagService =
      FeatureFlagService(
        repository,
        redisCache,
        slackNotifier
      )
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `create and retrieve feature flag`() {
    val name = RandomUtils.generateRandomString()
    val code = RandomUtils.generateRandomString()
    val metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0)
    val createdFlag =
      featureFlagService.createFeatureFlag(
        createFeatureFlagEntity(
          name = name,
          code = code,
          metadata = metadata
        )
      )

    assertNotNull(createdFlag)
    assertEquals(name, createdFlag.name)
    assertEquals(code, createdFlag.code)

    val retrievedFlag = featureFlagService.getFeatureFlagByCode(code)

    assertNotNull(retrievedFlag)
    assertEquals(name, retrievedFlag.name)
    assertEquals(code, retrievedFlag.code)
    assertEquals(true, retrievedFlag.enabled)
    assertEquals(metadata, retrievedFlag.metadata)

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.contains(code) && it.text.endsWith(" has been ${ChangeStatus.CREATED.name.lowercase()}")
        },
        match {
          it.containsKey("clientId") &&
            it.containsKey("apiKey") &&
            it["clientId"] == "test" &&
            it["apiKey"] == "1234"
        }
      )
    }
  }

  @Test
  fun `update feature flag`() {
    val code = RandomUtils.generateRandomString()
    val originalFlag =
      createFeatureFlagEntity(
        code = code,
        enabled = false,
        metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0)
      )

    featureFlagService.createFeatureFlag(originalFlag)

    val updatedName = RandomUtils.generateRandomString()
    val updatedFlag =
      originalFlag.copy(
        name = updatedName,
        enabled = true,
        metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0)
      )

    featureFlagService.updateFeatureFlag(code, updatedFlag)

    val retrievedFlag = featureFlagService.getFeatureFlagByCode(code)

    assertNotNull(retrievedFlag)
    assertEquals(updatedName, retrievedFlag.name)
    assertEquals(true, retrievedFlag.enabled)
    assertTrue(retrievedFlag.metadata is MetadataContent.GroupTargeting)

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.contains(code) && it.text.endsWith(" has been ${ChangeStatus.UPDATED.name.lowercase()}")
        },
        any()
      )
    }
  }

  @Test
  fun `delete feature flag`() {
    val featureFlag = FeatureFlag(name = "To Be Deleted", code = "DELETE_FLAG", enabled = true)

    featureFlagService.createFeatureFlag(featureFlag)
    featureFlagService.deleteFeatureFlag("DELETE_FLAG")

    assertThrows<FeatureFlagNotFoundError> {
      featureFlagService.getFeatureFlagByCode("DELETE_FLAG")
    }

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.endsWith(" has been ${ChangeStatus.DELETED.name.lowercase()}")
        },
        any()
      )
    }
  }

  @Test
  fun `list feature flags`() {
    val flag1 = FeatureFlag(name = "Flag 1", code = "FLAG_1", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))
    val flag2 = FeatureFlag(name = "Flag 2", code = "FLAG_2", enabled = false, metadata = MetadataContent.GroupTargeting(listOf("group1"), 60.0))

    featureFlagService.createFeatureFlag(flag1)
    featureFlagService.createFeatureFlag(flag2)

    val flags = featureFlagService.listFeatureFlags()

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
    featureFlagService.createFeatureFlag(userTargetedFlag)

    // user1 should be targeted because it is in the list and based on percentage
    assertTrue(featureFlagService.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user1")))

    // user3 is not in the user list, so it should not be targeted
    assertFalse(featureFlagService.isFeatureFlagEnabled("USER_FLAG", mapOf("userId" to "user3")))

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
    featureFlagService.createFeatureFlag(groupTargetedFlag)

    // group1 should be targeted because it is in the list and based on percentage
    assertTrue(featureFlagService.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group1")))

    // group3 is not in the group list, so it should not be targeted
    assertFalse(featureFlagService.isFeatureFlagEnabled("GROUP_FLAG", mapOf("groupId" to "group3")))

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
    featureFlagService.createFeatureFlag(timeBasedFlag)

    // Time-based flag should be enabled since current time is within the activation window
    assertTrue(featureFlagService.isFeatureFlagEnabled("TIME_FLAG", emptyMap()))
  }

  @Test
  fun `find feature flags by metadata type`() {
    val userFlag1 = FeatureFlag(name = "User Flag 1", code = "USER_FLAG_1", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0))
    val userFlag2 = FeatureFlag(name = "User Flag 2", code = "USER_FLAG_2", enabled = true, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user2"), percentage = 60.0))
    val groupFlag = FeatureFlag(name = "Group Flag", code = "GROUP_FLAG", enabled = true, metadata = MetadataContent.GroupTargeting(listOf("group1"), 70.0))

    featureFlagService.createFeatureFlag(userFlag1)
    featureFlagService.createFeatureFlag(userFlag2)
    featureFlagService.createFeatureFlag(groupFlag)

    val userFlags = featureFlagService.findFeatureFlagsByMetadataType("UserTargeting")
    assertEquals(2, userFlags.size)
    assertTrue(userFlags.all { it.metadata is MetadataContent.UserTargeting })

    val groupFlags = featureFlagService.findFeatureFlagsByMetadataType("GroupTargeting")
    assertEquals(1, groupFlags.size)
    assertTrue(groupFlags.all { it.metadata is MetadataContent.GroupTargeting })
  }

  @Test
  fun `test gradual rollout`() {
    val gradualRolloutFlag = FeatureFlag(name = "Gradual Rollout Flag", code = "GRADUAL_FLAG", enabled = true, metadata = MetadataContent.GradualRollout(startPercentage = 0.0, endPercentage = 100.0, startTime = Instant.now().minusSeconds(3600), duration = Duration.ofHours(2)))
    featureFlagService.createFeatureFlag(gradualRolloutFlag)

    // This test is probabilistic and may occasionally fail
    var enabledCount = 0
    for (i in 1..1000) {
      if (featureFlagService.isFeatureFlagEnabled("GRADUAL_FLAG", mapOf("userId" to "user$i"))) {
        enabledCount++
      }
    }

    // After 1 hour (half of the duration), we expect roughly 50% of users to have the flag enabled
    assertTrue(enabledCount in 400..600)
  }

  @Test
  fun `getMetadataValue should return null for feature flag with null metadata`() {
    val flag = createFeatureFlagEntity("Null Metadata", "NULL_METADATA", metadata = null)
    featureFlagService.createFeatureFlag(flag)
    assertNull(featureFlagService.getMetadataValue("NULL_METADATA", "anyKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for UserTargeting`() {
    val metadata = MetadataContent.UserTargeting(whitelistedUsers = mapOf("user1" to true, "user2" to false), blacklistedUsers = mapOf("user3" to true), targetedUserIds = listOf("user4", "user5"), percentage = 50.0, defaultValue = true)
    val flag = createFeatureFlagEntity("User Targeting", "USER_TARGETING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    assertEquals("user1:true,user2:false", featureFlagService.getMetadataValue("USER_TARGETING", "whitelistedUsers"))
    assertEquals("user3:true", featureFlagService.getMetadataValue("USER_TARGETING", "blacklistedUsers"))
    assertEquals("user4,user5", featureFlagService.getMetadataValue("USER_TARGETING", "targetedUserIds"))
    assertEquals("50.0", featureFlagService.getMetadataValue("USER_TARGETING", "percentage"))
    assertEquals("true", featureFlagService.getMetadataValue("USER_TARGETING", "defaultValue"))
    assertNull(featureFlagService.getMetadataValue("USER_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GroupTargeting`() {
    val metadata = MetadataContent.GroupTargeting(listOf("group1", "group2"), 75.0)
    val flag = createFeatureFlagEntity("Group Targeting", "GROUP_TARGETING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("group1,group2", featureFlagService.getMetadataValue("GROUP_TARGETING", "groupIds"))
    Assertions.assertEquals("75.0", featureFlagService.getMetadataValue("GROUP_TARGETING", "percentage"))
    assertNull(featureFlagService.getMetadataValue("GROUP_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for TimeBasedActivation`() {
    val startTime = Instant.parse("2023-01-01T00:00:00Z")
    val endTime = Instant.parse("2023-12-31T23:59:59Z")
    val metadata = MetadataContent.TimeBasedActivation(startTime, endTime)
    val flag = createFeatureFlagEntity("Time Based", "TIME_BASED", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals(startTime.toString(), featureFlagService.getMetadataValue("TIME_BASED", "startTime"))
    Assertions.assertEquals(endTime.toString(), featureFlagService.getMetadataValue("TIME_BASED", "endTime"))
    assertNull(featureFlagService.getMetadataValue("TIME_BASED", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GradualRollout`() {
    val startTime = Instant.parse("2023-01-01T00:00:00Z")
    val duration = Duration.ofDays(30)
    val metadata = MetadataContent.GradualRollout(0.0, 100.0, startTime, duration)
    val flag = createFeatureFlagEntity("Gradual Rollout", "GRADUAL_ROLLOUT", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("0.0", featureFlagService.getMetadataValue("GRADUAL_ROLLOUT", "startPercentage"))
    Assertions.assertEquals("100.0", featureFlagService.getMetadataValue("GRADUAL_ROLLOUT", "endPercentage"))
    Assertions.assertEquals(startTime.toString(), featureFlagService.getMetadataValue("GRADUAL_ROLLOUT", "startTime"))
    Assertions.assertEquals(duration.toString(), featureFlagService.getMetadataValue("GRADUAL_ROLLOUT", "duration"))
    assertNull(featureFlagService.getMetadataValue("GRADUAL_ROLLOUT", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for ABTestingConfig`() {
    val metadata = MetadataContent.ABTestingConfig("A", "B", 60.0)
    val flag = createFeatureFlagEntity("AB Testing", "AB_TESTING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("A", featureFlagService.getMetadataValue("AB_TESTING", "variantA"))
    Assertions.assertEquals("B", featureFlagService.getMetadataValue("AB_TESTING", "variantB"))
    Assertions.assertEquals("60.0", featureFlagService.getMetadataValue("AB_TESTING", "distribution"))
    assertNull(featureFlagService.getMetadataValue("AB_TESTING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for VersionTargeting`() {
    val metadata = MetadataContent.VersionTargeting("1.0.0", "2.0.0")
    val flag = createFeatureFlagEntity("Version Targeting", "VERSION_TARGETING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("1.0.0", featureFlagService.getMetadataValue("VERSION_TARGETING", "minVersion"))
    Assertions.assertEquals("2.0.0", featureFlagService.getMetadataValue("VERSION_TARGETING", "maxVersion"))
    assertNull(featureFlagService.getMetadataValue("VERSION_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for GeographicTargeting`() {
    val metadata = MetadataContent.GeographicTargeting(listOf("US", "UK"), listOf("CA", "NY"))
    val flag = createFeatureFlagEntity("Geographic Targeting", "GEO_TARGETING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("US,UK", featureFlagService.getMetadataValue("GEO_TARGETING", "countries"))
    Assertions.assertEquals("CA,NY", featureFlagService.getMetadataValue("GEO_TARGETING", "regions"))
    assertNull(featureFlagService.getMetadataValue("GEO_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for DeviceTargeting`() {
    val metadata = MetadataContent.DeviceTargeting(listOf("iOS", "Android"), listOf("Mobile", "Tablet"))
    val flag = createFeatureFlagEntity("Device Targeting", "DEVICE_TARGETING", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("iOS,Android", featureFlagService.getMetadataValue("DEVICE_TARGETING", "platforms"))
    Assertions.assertEquals("Mobile,Tablet", featureFlagService.getMetadataValue("DEVICE_TARGETING", "deviceTypes"))
    assertNull(featureFlagService.getMetadataValue("DEVICE_TARGETING", "invalidKey"))
  }

  @Test
  fun `getMetadataValue should return correct values for CustomRules`() {
    val metadata = MetadataContent.CustomRules(mapOf("rule1" to "value1", "rule2" to "value2"))
    val flag = createFeatureFlagEntity("Custom Rules", "CUSTOM_RULES", metadata = metadata)
    featureFlagService.createFeatureFlag(flag)

    Assertions.assertEquals("value1", featureFlagService.getMetadataValue("CUSTOM_RULES", "rule1"))
    Assertions.assertEquals("value2", featureFlagService.getMetadataValue("CUSTOM_RULES", "rule2"))
    assertNull(featureFlagService.getMetadataValue("CUSTOM_RULES", "invalidKey"))
  }

  @Test
  fun `enable and disable feature flag`() {
    val featureFlag = FeatureFlag(name = "Test Flag", code = "TOGGLE_FLAG", description = "A test flag for toggling", enabled = false)

    val createdFlag = featureFlagService.createFeatureFlag(featureFlag)
    assertFalse(createdFlag.enabled)

    val enabledFlag = featureFlagService.enableFeatureFlag("TOGGLE_FLAG")
    assertTrue(enabledFlag.enabled)

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.endsWith(" has been ${ChangeStatus.ENABLED.name.lowercase()}")
        },
        any()
      )
    }

    val disabledFlag = featureFlagService.disableFeatureFlag("TOGGLE_FLAG")
    assertFalse(disabledFlag.enabled)

    verify {
      mockSlackClient.sendMessage(
        match {
          it.text.endsWith(" has been ${ChangeStatus.DISABLED.name.lowercase()}")
        },
        any()
      )
    }

    assertThrows<FeatureFlagNotFoundError> {
      featureFlagService.enableFeatureFlag("NONEXISTENT_FLAG")
    }

    assertThrows<FeatureFlagNotFoundError> {
      featureFlagService.disableFeatureFlag("NONEXISTENT_FLAG")
    }
  }

  @Test
  fun `isFeatureFlagEnabled should respect enabled status`() {
    val featureFlag = FeatureFlag(name = "Always Off Flag", code = "ALWAYS_OFF", description = "A flag that's always off", enabled = false, metadata = MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 100.0))

    featureFlagService.createFeatureFlag(featureFlag)

    assertFalse(featureFlagService.isFeatureFlagEnabled("ALWAYS_OFF", mapOf("userId" to "user1")))

    featureFlagService.enableFeatureFlag("ALWAYS_OFF")

    assertTrue(featureFlagService.isFeatureFlagEnabled("ALWAYS_OFF", mapOf("userId" to "user1")))
  }

  private fun createFeatureFlagEntity(
    name: String = RandomUtils.generateRandomString(),
    code: String = RandomUtils.generateRandomString(),
    description: String? = RandomUtils.generateRandomString(),
    enabled: Boolean = true,
    metadata: MetadataContent? = null
  ): FeatureFlag =
    FeatureFlag(
      name = name,
      code = code,
      description = description,
      enabled = enabled,
      metadata = metadata
    )
}
