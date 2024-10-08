package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.client.SlackClient
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.NotifierError
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.notification.SlackNotifierConfig
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import java.io.IOException
import kotlin.test.assertFalse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Response

class SlackNotifierTest {
  private lateinit var slackClient: SlackClient
  private lateinit var slackNotifier: SlackNotifier
  private lateinit var config: SlackNotifierConfig

  @BeforeEach
  fun setup() {
    slackClient = mockk(relaxed = true)
    config =
      SlackNotifierConfig(
        webhookUrl = "https://hooks.slack.com/services/xxx/yyy/zzz",
        requestHeaders =
          mapOf(
            "apikey" to "test-api-key",
            "clientId" to "test-client-id"
          )
      )
    slackNotifier = SlackNotifier(config) { slackClient }
  }

  @AfterEach
  fun afterEach() {
    clearAllMocks()
  }

  @Test
  fun `send should successfully send notification when API call is successful`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.ENABLED

    coEvery {
      slackClient.sendMessage(any(), any())
    } returns Response.success(Unit)

    slackNotifier.send(featureFlag, changeStatus)
  }

  @Test
  fun `send should throw NotifierError when API call fails`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.ENABLED

    coEvery {
      slackClient.sendMessage(any(), any())
    } returns Response.error(400, mockk(relaxed = true))

    assertThrows<NotifierError> {
      slackNotifier.send(featureFlag, changeStatus)
    }
  }

  @Test
  fun `send should throw NotifierError when network error occurs`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.ENABLED

    coEvery {
      slackClient.sendMessage(any(), any())
    } throws IOException("Network error")

    assertThrows<NotifierError> {
      slackNotifier.send(featureFlag, changeStatus)
    }
  }

  @Test
  fun `send should not send notification for excluded status`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.UPDATED

    val configWithExclusion = config.copy(excludedStatuses = listOf(ChangeStatus.UPDATED))
    val notifierWithExclusion = SlackNotifier(configWithExclusion)

    var apiCalled = false
    coEvery {
      slackClient.sendMessage(any(), any())
    } answers {
      apiCalled = true
      Response.success(Unit)
    }

    notifierWithExclusion.send(featureFlag, changeStatus)

    assertFalse(apiCalled, "API should not have been called for excluded status")
  }
}
