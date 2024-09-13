package com.c0x12c.featureflag.service

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.exception.FeatureFlagNotFoundError
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeatureFlagIntegrationTest {

  companion object {
    private lateinit var database: Database
    private lateinit var repository: FeatureFlagRepository
    private lateinit var redisCache: RedisCache
    private lateinit var service: FeatureFlagService

    @BeforeAll
    @JvmStatic
    fun setup() {
      setupDatabase()
      setupRedisCache()

      repository = FeatureFlagRepository(database)
      service = FeatureFlagService(repository, redisCache)
    }

    private fun setupDatabase() {
      database = Database.connect(
        url = "jdbc:postgresql://localhost:5432/local",
        driver = "org.postgresql.Driver",
        user = "local",
        password = "local"
      )

      val setupSql = Files.readString(Paths.get("src/test/resources/setup.sql"))
      transaction(database) {
        exec(setupSql)
      }
    }

    private fun setupRedisCache() {
      val redisNodes = setOf(
        HostAndPort("localhost", 30001),
        HostAndPort("localhost", 30002),
        HostAndPort("localhost", 30003),
        HostAndPort("localhost", 30004),
        HostAndPort("localhost", 30005),
        HostAndPort("localhost", 30006)
      )
      val jedisCluster = JedisCluster(redisNodes)
      redisCache = RedisCache(jedisCluster, "test")
    }
  }

  @BeforeEach
  fun clearData() {
    transaction(database) {
      exec("TRUNCATE TABLE feature_flags")
    }

    redisCache.clearAll()
  }

  @Test
  fun `create and retrieve feature flag`() {
    val flagData = mapOf(
      "name" to "Test Flag",
      "code" to "TEST_FLAG",
      "description" to "A test flag",
      "enabled" to true,
      "metadata" to mapOf("key" to "value")
    )

    val createdFlag = service.createFeatureFlag(flagData)

    assertNotNull(createdFlag)
    assertEquals("Test Flag", createdFlag["name"])
    assertEquals("TEST_FLAG", createdFlag["code"])

    val retrievedFlag = service.getFeatureFlagByCode("TEST_FLAG")

    assertNotNull(retrievedFlag)
    assertEquals("Test Flag", retrievedFlag["name"])
    assertEquals("TEST_FLAG", retrievedFlag["code"])
    assertEquals(true, retrievedFlag["enabled"])
    assertEquals(mapOf("key" to "value"), retrievedFlag["metadata"])
  }

  @Test
  fun `update feature flag`() {
    val flagData = mapOf(
      "name" to "Original Flag",
      "code" to "UPDATE_FLAG",
      "enabled" to false
    )

    service.createFeatureFlag(flagData)

    service.updateFeatureFlag(
      "UPDATE_FLAG",
      mapOf(
        "name" to "Updated Flag",
        "enabled" to true
      )
    )

    val updatedFlag = service.getFeatureFlagByCode("UPDATE_FLAG")

    assertNotNull(updatedFlag)
    assertEquals("Updated Flag", updatedFlag["name"])
    assertEquals(true, updatedFlag["enabled"])
  }

  @Test
  fun `delete feature flag`() {
    val flagData = mapOf(
      "name" to "To Be Deleted",
      "code" to "DELETE_FLAG",
      "enabled" to true
    )

    service.createFeatureFlag(flagData)
    service.deleteFeatureFlag("DELETE_FLAG")

    assertThrows<FeatureFlagNotFoundError> {
      service.getFeatureFlagByCode("DELETE_FLAG")
    }
  }

  @Test
  fun `list feature flags`() {
    val flag1 = mapOf(
      "name" to "Flag 1",
      "code" to "FLAG_1",
      "enabled" to true
    )
    val flag2 = mapOf(
      "name" to "Flag 2",
      "code" to "FLAG_2",
      "enabled" to false
    )

    service.createFeatureFlag(flag1)
    service.createFeatureFlag(flag2)

    val flags = service.listFeatureFlags()

    assertEquals(2, flags.size)
    assertEquals("Flag 1", flags[0]["name"])
    assertEquals("Flag 2", flags[1]["name"])
  }

  @Test
  fun `enable and disable feature flag`() {
    val flagData = mapOf(
      "name" to "Toggle Flag",
      "code" to "TOGGLE_FLAG",
      "enabled" to false
    )

    service.createFeatureFlag(flagData)

    service.enableFeatureFlag("TOGGLE_FLAG")
    var flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(true, flag["enabled"])

    service.disableFeatureFlag("TOGGLE_FLAG")
    flag = service.getFeatureFlagByCode("TOGGLE_FLAG")
    assertEquals(false, flag["enabled"])
  }
}
