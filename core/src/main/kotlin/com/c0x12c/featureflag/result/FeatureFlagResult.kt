package com.c0x12c.featureflag.result

sealed class FeatureFlagResult<out T> {
  data class Success<T>(
    val data: T
  ) : FeatureFlagResult<T>()

  sealed class Error : FeatureFlagResult<Nothing>() {
    data class NotFound(
      val message: String
    ) : Error()

    data class GeneralError(
      val message: String
    ) : Error()
  }
}
