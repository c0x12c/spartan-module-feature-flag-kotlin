package com.c0x12c.featureflag.notification

import kotlinx.serialization.Serializable

@Serializable
data class SlackPayload(val text: String)
