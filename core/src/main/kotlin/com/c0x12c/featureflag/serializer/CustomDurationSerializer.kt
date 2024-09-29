package com.c0x12c.featureflag.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Duration

object CustomDurationSerializer : KSerializer<Duration> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CustomDuration", PrimitiveKind.STRING)

  override fun serialize(
    encoder: Encoder,
    value: Duration
  ) {
    encoder.encodeString(value.toString()) // Serialize Duration as ISO-8601 string
  }

  override fun deserialize(decoder: Decoder): Duration {
    return Duration.parse(decoder.decodeString()) // Parse ISO-8601 string to Duration
  }
}
