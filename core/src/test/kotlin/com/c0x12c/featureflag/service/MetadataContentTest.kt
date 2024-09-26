package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.models.MetadataContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataContentTest {
  @Test
  fun `UserTargeting should validate percentage range`() {
    MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 50.0) // Should not throw
    assertThrows<IllegalArgumentException> {
      MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = -1.0)
    }
    assertThrows<IllegalArgumentException> {
      MetadataContent.UserTargeting(targetedUserIds = listOf("user1"), percentage = 101.0)
    }
  }

  @Test
  fun `GroupTargeting should validate percentage range`() {
    MetadataContent.GroupTargeting(listOf("group1"), 75.0) // Should not throw
    assertThrows<IllegalArgumentException> {
      MetadataContent.GroupTargeting(listOf("group1"), -1.0)
    }
    assertThrows<IllegalArgumentException> {
      MetadataContent.GroupTargeting(listOf("group1"), 100.1)
    }
  }

  @Test
  fun `TimeBasedActivation should handle different time scenarios`() {
    val baseTime = Instant.parse("2023-01-01T00:00:00Z")
    val past = baseTime.minusSeconds(3600) // 1 hour before
    val future = baseTime.plusSeconds(3600) // 1 hour after

    val activationInPast = MetadataContent.TimeBasedActivation(past, baseTime)
    val activationInFuture = MetadataContent.TimeBasedActivation(baseTime, future)

    // Helper function to check if a time is within the activation period
    fun isWithinActivationPeriod(
      time: Instant,
      activation: MetadataContent.TimeBasedActivation
    ): Boolean = time > activation.startTime && time <= activation.endTime

    // Test past activation
    assertFalse(isWithinActivationPeriod(past, activationInPast), "Past should not be within past activation")
    assertTrue(isWithinActivationPeriod(past.plusSeconds(1), activationInPast), "Just after past should be within past activation")
    assertTrue(isWithinActivationPeriod(baseTime, activationInPast), "Base time should be within past activation")
    assertFalse(isWithinActivationPeriod(baseTime.plusSeconds(1), activationInPast), "Just after base time should not be within past activation")

    // Test future activation
    assertFalse(isWithinActivationPeriod(baseTime, activationInFuture), "Base time should not be within future activation")
    assertTrue(isWithinActivationPeriod(baseTime.plusSeconds(1), activationInFuture), "Just after base time should be within future activation")
    assertTrue(isWithinActivationPeriod(future, activationInFuture), "Future should be within future activation")
    assertFalse(isWithinActivationPeriod(future.plusSeconds(1), activationInFuture), "Just after future should not be within future activation")
  }

  @Test
  fun `GradualRollout should validate percentage ranges`() {
    val now = Instant.now()
    MetadataContent.GradualRollout(0.0, 100.0, now, Duration.ofDays(30)) // Should not throw
    assertThrows<IllegalArgumentException> {
      MetadataContent.GradualRollout(-1.0, 100.0, now, Duration.ofDays(30))
    }
    assertThrows<IllegalArgumentException> {
      MetadataContent.GradualRollout(0.0, 100.1, now, Duration.ofDays(30))
    }
  }

  @Test
  fun `ABTestingConfig should validate distribution range`() {
    MetadataContent.ABTestingConfig("A", "B", 50.0) // Should not throw
    assertThrows<IllegalArgumentException> {
      MetadataContent.ABTestingConfig("A", "B", -1.0)
    }
    assertThrows<IllegalArgumentException> {
      MetadataContent.ABTestingConfig("A", "B", 100.1)
    }
  }

  @Test
  fun `VersionTargeting should handle version comparisons correctly`() {
    val targeting = MetadataContent.VersionTargeting("1.0.0", "2.0.0")
    assertTrue("1.5.0" in targeting.minVersion..targeting.maxVersion)
    assertFalse("0.9.9" in targeting.minVersion..targeting.maxVersion)
    assertFalse("2.0.1" in targeting.minVersion..targeting.maxVersion)
  }

  @Test
  fun `GeographicTargeting should handle country and region lists`() {
    val targeting =
      MetadataContent.GeographicTargeting(
        countries = listOf("US", "CA"),
        regions = listOf("NA", "EU")
      )
    assertEquals(2, targeting.countries.size)
    assertEquals(2, targeting.regions.size)
    assertTrue("US" in targeting.countries)
    assertTrue("EU" in targeting.regions)
  }

  @Test
  fun `DeviceTargeting should handle platform and device type lists`() {
    val targeting =
      MetadataContent.DeviceTargeting(
        platforms = listOf("iOS", "Android"),
        deviceTypes = listOf("Mobile", "Tablet")
      )
    assertEquals(2, targeting.platforms.size)
    assertEquals(2, targeting.deviceTypes.size)
    assertTrue("iOS" in targeting.platforms)
    assertTrue("Tablet" in targeting.deviceTypes)
  }

  @Test
  fun `CustomRules should handle key-value pairs`() {
    val rules =
      MetadataContent.CustomRules(
        rules =
          mapOf(
            "subscriptionTier" to "premium",
            "hasCompletedOnboarding" to "true"
          )
      )
    assertEquals(2, rules.rules.size)
    assertEquals("premium", rules.rules["subscriptionTier"])
    assertEquals("true", rules.rules["hasCompletedOnboarding"])
  }
}
