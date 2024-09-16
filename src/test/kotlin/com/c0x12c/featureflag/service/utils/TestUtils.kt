package com.c0x12c.featureflag.service.utils

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.service.FeatureFlagService
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import java.nio.file.Files
import java.nio.file.Paths

object TestUtils {
  lateinit var database: Database
  lateinit var repository: FeatureFlagRepository
  lateinit var redisCache: RedisCache
  lateinit var service: FeatureFlagService

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

  fun clearData() {
    transaction(database) {
      exec("TRUNCATE TABLE feature_flags")
    }

    redisCache.clearAll()
  }
}
