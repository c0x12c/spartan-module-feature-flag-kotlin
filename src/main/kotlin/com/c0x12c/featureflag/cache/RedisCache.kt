package com.c0x12c.featureflag.cache

import com.c0x12c.featureflag.entity.FeatureFlag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisCluster

class RedisCache(
  private val jedisCluster: JedisCluster,
  private val keyspace: String
) {
  private fun keyFrom(key: String): String {
    return "$keyspace:$key"
  }

  fun set(key: String, value: FeatureFlag, ttlSeconds: Long): Boolean {
    return try {
      val serializedValue = Json.encodeToString(value)
      val redisKey = keyFrom(key)
      jedisCluster.setex(redisKey, ttlSeconds, serializedValue)
      true
    } catch (e: Exception) {
      false
    }
  }

  fun get(key: String): FeatureFlag? {
    return try {
      val result = jedisCluster.get(keyFrom(key)) ?: return null
      Json.decodeFromString<FeatureFlag>(result)
    } catch (e: Exception) {
      null
    }
  }

  fun delete(key: String): Boolean {
    return try {
      jedisCluster.del(keyFrom(key)) > 0
    } catch (e: Exception) {
      false
    }
  }

  fun clearAll(): Boolean {
    return try {
      val keys = jedisCluster.keys("$keyspace:*")
      if (keys.isNotEmpty()) {
        jedisCluster.del(*keys.toTypedArray())
      }
      true
    } catch (e: Exception) {
      false
    }
  }
}
