# MetadataContent Usage Guide

The `MetadataContent` sealed class provides a flexible way to define various targeting and configuration options for feature flags. This guide will walk you through each subclass and provide examples of how to use them.

# MetadataContent Usage Guide

## 1. UserTargeting

Use this when you want to target specific users, exclude certain users, or target a percentage of users with more granular control.

```kotlin
val userTargeting = MetadataContent.UserTargeting(
    whitelistedUsers = mapOf("user1" to true, "user2" to false),
    blacklistedUsers = mapOf("user3" to true, "user4" to false),
    targetedUserIds = listOf("user5", "user6", "user7"),
    percentage = 25.0,
    defaultValue = false
)
```

This configuration provides the following targeting options:

- `whitelistedUsers`: A map of user IDs to boolean values. Users with `true` values will always have the feature enabled, regardless of other settings.
- `blacklistedUsers`: A map of user IDs to boolean values. Users with `true` values will always have the feature disabled, regardless of other settings.
- `targetedUserIds`: A list of user IDs that will be included in the percentage-based targeting.
- `percentage`: The percentage of users (from the `targetedUserIds` list) who should have the feature enabled. Must be between 0.0 and 100.0.
- `defaultValue`: The default state of the feature for users not specifically targeted or when the user doesn't meet the percentage criteria.

The targeting logic works as follows:

1. If a user is in `whitelistedUsers` with a `true` value, the feature is enabled.
2. If a user is in `blacklistedUsers` with a `true` value, the feature is disabled.
3. If a user is in `targetedUserIds`, they are subject to the percentage-based targeting.
4. For users in `targetedUserIds`, the feature is enabled for the specified `percentage` of users (calculated using a hash of the user ID for consistency).
5. For all other users, the `defaultValue` is used.

Example usage:

```kotlin
val featureFlag = FeatureFlag(
    name = "New Dashboard",
    code = "new_dashboard_feature",
    description = "Targeted rollout of the new dashboard",
    enabled = true,
    metadata = MetadataContent.UserTargeting(
        whitelistedUsers = mapOf("admin1" to true, "beta_tester1" to true),
        blacklistedUsers = mapOf("problem_user1" to true),
        targetedUserIds = listOf("user1", "user2", "user3", "user4"),
        percentage = 50.0,
        defaultValue = false
    )
)

featureFlagService.createFeatureFlag(featureFlag)
```

This creates a new feature flag for a targeted rollout of a new dashboard feature:
- It's always enabled for "admin1" and "beta_tester1".
- It's always disabled for "problem_user1".
- For users "user1", "user2", "user3", and "user4", it's enabled for 50% of them.
- For all other users, it's disabled by default.

Note: The `percentage` must be between 0.0 and 100.0. An `IllegalArgumentException` will be thrown if this requirement is not met.

## 2. GroupTargeting

Similar to UserTargeting, but for groups instead of individual users.

```kotlin
val groupTargeting = MetadataContent.GroupTargeting(
    groupIds = listOf("group1", "group2"),
    percentage = 50.0
)
```

This enables the feature for all users in "group1" or "group2", and for 50% of users in other groups. The percentage is calculated using a hash of the group ID for consistency.

## 3. TimeBasedActivation

Use this to activate a feature during a specific time window.

```kotlin
val timeBasedActivation = MetadataContent.TimeBasedActivation(
    startTime = Instant.parse("2023-06-01T00:00:00Z"),
    endTime = Instant.parse("2023-06-30T23:59:59Z")
)
```

This configuration will enable the feature only during June 2023.

## 4. GradualRollout

Use this for a gradual rollout of a feature over time.

```kotlin
val gradualRollout = MetadataContent.GradualRollout(
    startPercentage = 10.0,
    endPercentage = 100.0,
    startTime = Instant.parse("2023-06-01T00:00:00Z"),
    duration = Duration.ofDays(30)
)
```

This configuration starts with 10% of users on June 1st and gradually increases to 100% over 30 days. The rollout is based on a hash of the user ID for consistency.

## 5. ABTestingConfig

Use this for A/B testing different variants of a feature.

```kotlin
val abTestingConfig = MetadataContent.ABTestingConfig(
    variantA = "blue_button",
    variantB = "red_button",
    distribution = 50.0
)
```

This configuration will show the "blue_button" variant to 50% of users and the "red_button" variant to the other 50%. The distribution is based on a hash of the user ID for consistency.

## 6. VersionTargeting

Use this to target specific versions of your application.

```kotlin
val versionTargeting = MetadataContent.VersionTargeting(
    minVersion = "1.0.0",
    maxVersion = "2.0.0"
)
```

This configuration enables the feature for app versions between 1.0.0 and 2.0.0 (inclusive). Version comparison is done using a `ComparableVersion` class for accurate semantic versioning.

## 7. GeographicTargeting

Use this to target users in specific countries or regions.

```kotlin
val geographicTargeting = MetadataContent.GeographicTargeting(
    countries = listOf("US", "CA", "MX"),
    regions = listOf("NA", "EU")
)
```

This configuration enables the feature for users in the specified countries or regions. The behavior can be controlled using a `checkBoth` flag in the context:

- If `checkBoth` is `true`, both the country AND region must match.
- If `checkBoth` is `false` (default), either the country OR region must match.

Example usage:

```kotlin
val context = mapOf(
    "country" to "US",
    "region" to "NA",
    "checkBoth" to true  // or false
)
featureFlagService.isFeatureFlagEnabled("geo_feature", context)
```

## 8. DeviceTargeting

Use this to target specific platforms or device types.

```kotlin
val deviceTargeting = MetadataContent.DeviceTargeting(
    platforms = listOf("iOS", "Android"),
    deviceTypes = listOf("Mobile", "Tablet")
)
```

This configuration enables the feature for specific platforms and device types. Similar to GeographicTargeting, it uses a `checkBoth` flag in the context:

- If `checkBoth` is `true`, both the platform AND device type must match.
- If `checkBoth` is `false` (default), either the platform OR device type must match.

Example usage:

```kotlin
val context = mapOf(
    "platform" to "iOS",
    "deviceType" to "Mobile",
    "checkBoth" to true  // or false
)
featureFlagService.isFeatureFlagEnabled("device_feature", context)
```

## 9. CustomRules

Use this for any custom targeting logic specific to your application.

```kotlin
val customRules = MetadataContent.CustomRules(
    rules = mapOf(
        "subscriptionTier" to "premium",
        "hasCompletedOnboarding" to "true"
    )
)
```

This configuration enables the feature for users with a premium subscription who have completed the onboarding process. The rules are case-insensitive for more flexible matching.

## Using MetadataContent with FeatureFlags

When creating or updating a feature flag, you can use these MetadataContent configurations to define complex targeting rules. Here's an example of how you might use it with a FeatureFlag class:

```kotlin
val featureFlag = FeatureFlag(
    name = "New UI",
    code = "new_ui_feature",
    description = "Gradual rollout of the new UI",
    enabled = true,
    metadata = MetadataContent.GradualRollout(
        startPercentage = 10.0,
        endPercentage = 100.0,
        startTime = Instant.parse("2023-06-01T00:00:00Z"),
        duration = Duration.ofDays(30)
    )
)

featureFlagService.createFeatureFlag(featureFlag)
```

This creates a new feature flag for a gradual rollout of a new UI feature, starting with 10% of users and increasing to 100% over 30 days beginning on June 1st, 2023.

## Important Notes

1. For UserTargeting and GroupTargeting, the percentage-based targeting uses a MurmurHash of the user ID or group ID for consistent distribution.
2. For GeographicTargeting and DeviceTargeting, you can specify whether to check both criteria (e.g., country AND region) or either criterion (country OR region) by setting a `checkBoth` flag in the context.
3. Version targeting uses a `ComparableVersion` class for accurate semantic version comparisons.
4. Custom rules are now case-insensitive for more flexible matching.
5. All percentage values (in UserTargeting, GroupTargeting, GradualRollout, and ABTestingConfig) must be between 0.0 and 100.0.