package com.c0x12c.featureflag.service.cache

import com.c0x12c.featureflag.cache.RedisCache
import com.c0x12c.featureflag.entity.FeatureFlag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisCluster

class JedisClusterCache(
  private val jedisCluster: JedisCluster,
  private val keyspace: String,
  private val ttlSeconds: Long = 3600
) : RedisCache {
  private fun keyFrom(key: String): String = "$keyspace:$key"

  private fun serialize(featureFlag: FeatureFlag): String = Json.encodeToString(featureFlag)

  private fun deserialize(data: String): FeatureFlag = Json.decodeFromString<FeatureFlag>(data)

  override fun set(
    key: String,
    value: FeatureFlag
  ): Boolean =
    try {
      val redisKey = keyFrom(key)
      jedisCluster.setex(redisKey, ttlSeconds, serialize(value))
      true
    } catch (e: Exception) {
      false
    }

  override fun get(key: String): FeatureFlag? {
    return try {
      val result = jedisCluster.get(keyFrom(key)) ?: return null
      deserialize(result)
    } catch (e: Exception) {
      null
    }
  }

  override fun delete(key: String): Boolean =
    try {
      jedisCluster.del(keyFrom(key)) > 0
    } catch (e: Exception) {
      false
    }

  override fun clearAll(): Boolean =
    try {
      val keys = jedisCluster.keys("$keyspace:*")
      if (keys.isNotEmpty()) {
        jedisCluster.del(*keys.toTypedArray())
      }
      true
    } catch (e: Exception) {
      false
    }
}
