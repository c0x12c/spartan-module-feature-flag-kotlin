import com.c0x12c.featureflag.entity.FeatureFlag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import redis.clients.jedis.JedisCluster

class RedisCache(
  private val jedisCluster: JedisCluster,
  private val keyspace: String,
  private val ttlSeconds: Long = 3600
) {
  private fun keyFrom(key: String): String = "$keyspace:$key"

  private fun serializeFeatureFlag(featureFlag: FeatureFlag): String = Json.encodeToString(featureFlag)

  private fun deserializeFeatureFlag(data: String): FeatureFlag = Json.decodeFromString<FeatureFlag>(data)

  fun set(
    key: String,
    value: FeatureFlag
  ): Boolean =
    try {
      val redisKey = keyFrom(key)
      jedisCluster.setex(redisKey, ttlSeconds, serializeFeatureFlag(value))
      true
    } catch (e: Exception) {
      false
    }

  fun get(key: String): FeatureFlag? {
    return try {
      val result = jedisCluster.get(keyFrom(key)) ?: return null
      deserializeFeatureFlag(result)
    } catch (e: Exception) {
      null
    }
  }

  fun delete(key: String): Boolean =
    try {
      jedisCluster.del(keyFrom(key)) > 0
    } catch (e: Exception) {
      false
    }

  fun clearAll(): Boolean =
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
