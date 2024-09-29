package com.c0x12c.featureflag.exception

/**
 * Base exception class for feature flag related errors.
 *
 * @param message The error message.
 * @param cause The cause of the error (optional).
 */
open class FeatureFlagError(
  message: String,
  cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when a requested feature flag is not found.
 *
 * @param message The error message.
 * @param cause The cause of the error (optional).
 */
class FeatureFlagNotFoundError(
  message: String,
  cause: Throwable? = null
) : FeatureFlagError(message, cause)

/**
 * Exception thrown when there is an error in the notification process.
 *
 * @param message The error message.
 * @param cause The cause of the error (optional).
 */
class NotifierError(
  message: String,
  cause: Throwable? = null
) : FeatureFlagError(message, cause)
