package com.c0x12c.featureflag.notification

import com.c0x12c.featureflag.client.SlackClient
import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.exception.NotifierError

class SlackNotifier(
  private val config: SlackNotifierConfig,
  private val clientProvider: (() -> SlackClient)? = null
) {
  private val client: SlackClient by lazy {
    clientProvider?.invoke() ?: createSlackClient(config)
  }

  fun send(
    featureFlag: FeatureFlag,
    changeStatus: ChangeStatus
  ) {
    if (changeStatus in config.excludedStatuses) return

    val message = buildMessage(featureFlag, changeStatus)
    val payload = SlackPayload(message)

    try {
      val response = client.sendMessage(payload, config.requestHeaders)

      if (!response.isSuccessful) {
        throw NotifierError("Failed to send Slack notification with [code=${response.code()}, message='${response.message()}]")
      }
    } catch (e: Exception) {
      throw NotifierError("Failed to send Slack notification: ${e.message}", e)
    }
  }

  private fun buildMessage(
    featureFlag: FeatureFlag,
    changeStatus: ChangeStatus
  ): String = "Feature Flag[Code=`${featureFlag.code}`] has been ${changeStatus.name.lowercase()}"

  private fun createSlackClient(config: SlackNotifierConfig): SlackClient = SlackClient.create(webhookUrl = config.webhookUrl)
}
