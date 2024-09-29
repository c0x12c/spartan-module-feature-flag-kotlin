package com.c0x12c.featureflag.service.utils

class RandomUtils {
  companion object {
    val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

    fun generateRandomString(length: Int = 8): String =
      (1..length)
        .map { CHARS.random() } // Pick a random character from the set
        .joinToString("")
  }
}
