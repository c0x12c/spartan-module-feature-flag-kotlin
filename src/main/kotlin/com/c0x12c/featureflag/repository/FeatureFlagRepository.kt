package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.entity.FeatureFlags
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class FeatureFlagRepository(private val database: Database) {

  fun insert(flag: FeatureFlagEntity): UUID {
    return transaction(database) {
      FeatureFlagEntity.new {
        name = flag.name
        code = flag.code
        description = flag.description
        enabled = flag.enabled
        metadata = flag.metadata
        createdAt = flag.createdAt
        updatedAt = flag.updatedAt
        deletedAt = flag.deletedAt
      }.id.value
    }
  }

  fun getById(id: UUID): FeatureFlagEntity? {
    return transaction(database) {
      FeatureFlagEntity.findById(id)
    }
  }

  fun getByCode(code: String): FeatureFlagEntity? {
    return transaction(database) {
      FeatureFlagEntity.find { FeatureFlags.code eq code }.singleOrNull()
    }
  }

  fun list(limit: Int = 100, offset: Int = 0): List<FeatureFlagEntity> {
    return transaction(database) {
      FeatureFlagEntity.all().limit(limit, offset.toLong()).toList()
    }
  }

  fun update(flag: FeatureFlagEntity) {
    transaction(database) {
      flag.id.let { id ->
        FeatureFlagEntity.findById(id)?.apply {
          name = flag.name
          code = flag.code
          description = flag.description
          enabled = flag.enabled
          metadata = flag.metadata
          updatedAt = flag.updatedAt
          deletedAt = flag.deletedAt
        }
      }
    }
  }

  fun delete(id: UUID) {
    transaction(database) {
      FeatureFlagEntity.findById(id)?.delete()
    }
  }
}