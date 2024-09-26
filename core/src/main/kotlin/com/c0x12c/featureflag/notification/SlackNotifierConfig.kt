package com.c0x12c.featureflag.notification

data class SlackNotifierConfig(
  val webhookUrl: String,
  val apiKey: String? = null,
  val clientId: String? = null,
  val excludedStatuses: List<ChangeStatus> = emptyList()
)
