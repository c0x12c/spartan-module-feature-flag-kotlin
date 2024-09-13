package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.cache.RedisCache
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureFlagCacheServiceIntegrationTest {

    private lateinit var postgresContainer: PostgreSQLContainer<*>
    private lateinit var redisContainer: GenericContainer<*>
    private lateinit var redisClient: RedisClient
    private lateinit var redisConnection: StatefulRedisConnection<String, String>
    private lateinit var repository: FeatureFlagRepository
    private lateinit var cache: RedisCache
    private lateinit var service: FeatureFlagService

    @BeforeAll
    fun setUpContainers() {
        // Start PostgreSQL container
        postgresContainer = PostgreSQLContainer<Nothing>("postgres:13").apply {
            withDatabaseName("local")
            withUsername("local")
            withPassword("local")
            start()
        }

        // Run the setup.sql script to create the feature_flags table
        runSqlScript(postgresContainer.jdbcUrl, postgresContainer.username, postgresContainer.password, "/setup.sql")

        // Start Redis container
        redisContainer = GenericContainer(DockerImageName.parse("redis:6")).apply {
            withExposedPorts(6379)
            start()
        }

        // Set up Redis client and connection
        redisClient = RedisClient.create("redis://${redisContainer.host}:${redisContainer.getMappedPort(6379)}")
        redisConnection = redisClient.connect()

        // Initialize repository and cache
        repository = FeatureFlagRepository(
            jdbcUrl = postgresContainer.jdbcUrl,
            username = postgresContainer.username,
            password = postgresContainer.password
        )
        cache = RedisCache(redisConnection)

        // Initialize the FeatureFlagService
        service = FeatureFlagService(repository, cache)
    }

    // Helper method to run an SQL script from the classpath
    private fun runSqlScript(jdbcUrl: String, username: String, password: String, classpathResource: String) {
        val sql = this::class.java.getResource(classpathResource)?.readText()
            ?: throw IllegalArgumentException("SQL file not found in classpath: $classpathResource")

        DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(sql)  // Execute the SQL script
            }
        }
    }

    @AfterAll
    fun tearDownContainers() {
        redisConnection.close()
        redisClient.shutdown()
        redisContainer.stop()
        postgresContainer.stop()
    }

    @Test
    fun `create and retrieve feature flag`() = runBlocking {
        // Arrange: Generate random values for name and code
        val randomName = "Test Feature ${UUID.randomUUID()}"
        val randomCode = "test_feature_${UUID.randomUUID()}"

        val flagData = mapOf("name" to randomName, "code" to randomCode, "enabled" to true)

        // Act: Create a feature flag
        val createdFlag = service.createFeatureFlag(flagData)

        // Assert: Check that the flag is created correctly
        assertNotNull(createdFlag)
        assertEquals(randomName, createdFlag.name)
        assertEquals(randomCode, createdFlag.code)
        assertTrue(createdFlag.enabled)

        // Act: Retrieve the flag from the service
        val retrievedFlag = service.getFeatureFlagByCode(randomCode)

        // Assert: Verify that the retrieved flag matches the created one
        assertNotNull(retrievedFlag)
        assertEquals(createdFlag, retrievedFlag)
    }

    @Test
    fun `update feature flag`() = runBlocking {
        // Arrange: Generate random values for name and code
        val randomName = "Test Feature ${UUID.randomUUID()}"
        val randomCode = "test_feature_${UUID.randomUUID()}"

        val flagData = mapOf("name" to randomName, "code" to randomCode, "enabled" to false)
        val createdFlag = service.createFeatureFlag(flagData)

        // Act: Update the feature flag
        val updatedFlagData = mapOf("name" to "Updated Feature ${UUID.randomUUID()}", "enabled" to true)
        service.updateFeatureFlag(createdFlag.code, updatedFlagData)

        // Assert: Retrieve the updated flag and verify the changes
        val updatedFlag = service.getFeatureFlagByCode(randomCode)
        assertNotNull(updatedFlag)
        assertTrue(updatedFlag?.enabled ?: false)
        assertTrue(updatedFlag?.name?.startsWith("Updated Feature") ?: false)
    }

    @Test
    fun `delete feature flag`() = runBlocking {
        // Arrange: Generate random values for name and code
        val randomName = "Test Feature ${UUID.randomUUID()}"
        val randomCode = "test_feature_${UUID.randomUUID()}"

        val flagData = mapOf("name" to randomName, "code" to randomCode, "enabled" to true)
        service.createFeatureFlag(flagData)

        // Act: Delete the feature flag
        service.deleteFeatureFlag(randomCode)

        // Assert: Ensure the flag is deleted from both repository and cache
        val deletedFlag = service.getFeatureFlagByCode(randomCode)
        assertNull(deletedFlag)
    }
}
