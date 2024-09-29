# Module Feature Flag

This module provides a flexible and standardized way to manage feature flags in Kotlin applications. It supports PostgreSQL for persistent storage and optional Redis caching for improved performance.

## Features
- **CRUD Operations**: Create, read, update, and delete feature flags.
- **Database Support**: PostgreSQL integration using Exposed ORM.
- **Optional Redis Caching**: Cache feature flag data for faster access. Redis integration is optional and can be omitted.
- **Serialization**: Kotlin serialization for efficient data handling.
- **Exception Handling**: Custom exceptions for better error management.

## Installation
To install the module and its dependencies, use your preferred Kotlin dependency management tool (e.g., Gradle or Maven).

Gradle example:
```kotlin
dependencies {
    implementation("com.c0x12c:feature-flag:1.0.0")
}
```

## Example Usage
Here's a basic example of how to use the Feature Flag module:

```kotlin
import com.c0x12c.featureflag.service.FeatureFlagService
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.cache.RedisCache
import org.jetbrains.exposed.sql.Database

// Initialize database connection
val database = Database.connect(...)

// Initialize repository and cache
val repository = FeatureFlagRepository(database)
val cache = RedisCache(...)

// Create FeatureFlagService
val featureFlagService = FeatureFlagService(repository, cache)

// Create a new feature flag
val newFlag = featureFlagService.createFeatureFlag(mapOf(
    "name" to "New Feature",
    "code" to "NEW_FEATURE",
    "description" to "A new feature flag",
    "enabled" to false
))

// Get a feature flag
val flag = featureFlagService.getFeatureFlagByCode("NEW_FEATURE")

// Update a feature flag
featureFlagService.updateFeatureFlag("NEW_FEATURE", mapOf("enabled" to true))

// Delete a feature flag
featureFlagService.deleteFeatureFlag("NEW_FEATURE")
```

## Development

### Debugging & Fixing Issues:
Local Debugging: Clone the module repository and import it into your IDE as a Kotlin project.

### Install
Ensure you have the Kotlin compiler and your preferred build tool (Gradle or Maven) installed.

### Run docker-compose
To run the PostgreSQL and Redis services for development, use:
```bash
docker-compose -f docker-compose.dev.yml up -d
```

To shut down the services, use:
```bash
docker-compose -f docker-compose.dev.yml down
```

### Testing
To run the tests, use your build tool's test command. For example, with Gradle:
```bash
./gradlew test
```

## Release
Please follow guidelines in [docs/RELEASE.md](./docs/RELEASE.md)
