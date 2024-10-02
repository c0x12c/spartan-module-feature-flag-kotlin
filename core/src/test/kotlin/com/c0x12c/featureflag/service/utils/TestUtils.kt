package com.c0x12c.featureflag.service.utils

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.repository.FeatureFlagRepository
import com.c0x12c.featureflag.service.cache.JedisClusterCache
import java.nio.file.Files
import java.nio.file.Paths
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster

object TestUtils {
  private lateinit var database: Database
  lateinit var repository: FeatureFlagRepository
  lateinit var redisCache: RedisCache

  private const val REDIS_HOST = "localhost"
  private const val DB_URL = "jdbc:postgresql://localhost:5432/local"
  private const val DB_DRIVER = "org.postgresql.Driver"
  private const val DB_USER = "local"
  private const val DB_PASSWORD = "local"
  private val REDIS_PORTS = setOf(30001, 30002, 30003, 30004, 30005, 30006)

  fun setupDependencies() {
    setupDatabase()
    setupRedisCache()
    repository = FeatureFlagRepository(database)
  }

  private fun setupDatabase() {
    database = Database.connect(url = DB_URL, driver = DB_DRIVER, user = DB_USER, password = DB_PASSWORD)
    val setupSql = Files.readString(Paths.get("src/test/resources/setup.sql"))
    transaction(database) { exec(setupSql) }
  }

  private fun setupRedisCache() {
    val redisNodes = REDIS_PORTS.map { createHostAndPort(it) }.toSet()
    val jedisCluster = JedisCluster(redisNodes)
    redisCache = JedisClusterCache(jedisCluster, "test")
  }

  private fun createHostAndPort(port: Int) = HostAndPort(REDIS_HOST, port)

  fun clearData() {
    transaction(database) { exec("TRUNCATE TABLE feature_flags") }
    redisCache.clearAll()
  }

  fun cleanDependencies() {
    cleanDatabase()
  }

  private fun cleanDatabase() {
    database = Database.connect(url = DB_URL, driver = DB_DRIVER, user = DB_USER, password = DB_PASSWORD)
    val cleanSql = Files.readString(Paths.get("src/test/resources/clean.sql"))
    transaction(database) { exec(cleanSql) }
  }
}
