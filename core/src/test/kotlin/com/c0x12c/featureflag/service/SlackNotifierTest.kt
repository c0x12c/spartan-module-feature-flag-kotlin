package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.NotifierError
import com.c0x12c.featureflag.notification.ChangeStatus
import com.c0x12c.featureflag.notification.SlackApi
import com.c0x12c.featureflag.notification.SlackNotifier
import com.c0x12c.featureflag.notification.SlackNotifierConfig
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertFalse

class SlackNotifierTest {
  private lateinit var slackApi: SlackApi
  private lateinit var slackNotifier: SlackNotifier
  private lateinit var config: SlackNotifierConfig

  @BeforeEach
  fun setup() {
    slackApi = mockk()
    config =
      SlackNotifierConfig(
        webhookUrl = "https://hooks.slack.com/services/xxx/yyy/zzz",
        apiKey = "test-api-key",
        clientId = "test-client-id"
      )
    slackNotifier = SlackNotifier(config, slackApi)
  }

  @Test
  fun `send should successfully send notification when API call is successful`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.ENABLED

    coEvery {
      slackApi.sendMessage(any(), any(), any())
    } returns Response.success(Unit)

    // This should not throw an exception
    slackNotifier.send(featureFlag, changeStatus)
  }

  @Test
  fun `send should throw NotifierError when API call fails`() {
    val featureFlag = FeatureFlag(code = "TEST_FLAG", name = "Test Flag")
    val changeStatus = ChangeStatus.ENABLED

    coEvery {
      slackApi.sendMessage(any(), any(), any())
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
      slackApi.sendMessage(any(), any(), any())
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
    val notifierWithExclusion = SlackNotifier(configWithExclusion, slackApi)

    var apiCalled = false
    coEvery {
      slackApi.sendMessage(any(), any(), any())
    } answers {
      apiCalled = true
      Response.success(Unit)
    }

    notifierWithExclusion.send(featureFlag, changeStatus)

    assertFalse(apiCalled, "API should not have been called for excluded status")
  }
}
