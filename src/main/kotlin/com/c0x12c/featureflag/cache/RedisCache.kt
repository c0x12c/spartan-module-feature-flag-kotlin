package com.c0x12c.featureflag.cache

import com.c0x12c.featureflag.entity.FeatureFlag
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RedisCache(private val redisConnection: StatefulRedisConnection<String, String>) {

    fun set(key: String, value: FeatureFlag) {
        val redisCommands = redisConnection.sync()
        // Serialize FeatureFlag object to JSON string
        val serializedValue = Json.encodeToString(value)
        redisCommands.set(key, serializedValue)
    }

    fun get(key: String): FeatureFlag? {
        val redisCommands = redisConnection.sync()
        val result = redisCommands.get(key) ?: return null
        // Deserialize JSON string back into a FeatureFlag object
        return Json.decodeFromString(result)
    }

    fun delete(key: String) {
        val redisCommands = redisConnection.sync()
        redisCommands.del(key)
    }
}
