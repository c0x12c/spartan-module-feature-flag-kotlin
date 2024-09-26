package com.c0x12c.featureflag.service.utils

class RandomUtils {
  fun generateRandomString(length: Int): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length)
      .map { chars.random() } // Pick a random character from the set
      .joinToString("")
  }
}
