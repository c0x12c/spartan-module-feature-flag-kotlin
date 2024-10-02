package com.c0x12c.featureflag.cache

import com.c0x12c.featureflag.entity.FeatureFlag

interface RedisCache {
  fun set(
    key: String,
    value: FeatureFlag
  )

  fun get(key: String): FeatureFlag?

  fun delete(key: String)

  fun clearAll()
}
