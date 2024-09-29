package com.c0x12c.featureflag.notification

data class SlackNotifierConfig(
  val webhookUrl: String,
  val excludedStatuses: List<ChangeStatus> = emptyList(),
  var requestHeaders: Map<String, String> = emptyMap()
)
