package com.c0x12c.featureflag.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Manifest(
  val name: String,
  @SerialName("release_strategy")
  val releaseStrategy: String,
  val version: String,
  val language: String
)
