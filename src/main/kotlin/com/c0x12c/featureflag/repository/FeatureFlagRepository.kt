package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.table.FeatureFlagTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class FeatureFlagRepository(private val database: Database) {

  fun insert(featureFlag: FeatureFlag): UUID {
    return transaction(database) {
      FeatureFlagEntity.new {
        name = featureFlag.name
        code = featureFlag.code
        description = featureFlag.description
        enabled = featureFlag.enabled
        metadata = featureFlag.metadata
        createdAt = Instant.now()
      }.id.value
    }
  }

  fun getById(id: UUID): FeatureFlag? {
    return transaction(database) {
      FeatureFlagEntity.findById(id)?.toFeatureFlag()
    }
  }

  fun list(limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return transaction(database) {
      FeatureFlagEntity.all().limit(limit, offset.toLong()).map { it.toFeatureFlag() }
    }
  }

  fun update(code: String, featureFlag: FeatureFlag): FeatureFlag? {
    return transaction(database) {
      val flag = FeatureFlagEntity.find { FeatureFlagTable.code eq code }.singleOrNull() ?: return@transaction null

      flag.apply {
        name = featureFlag.name
        description = featureFlag.description
        enabled = featureFlag.enabled
        metadata = featureFlag.metadata
        updatedAt = Instant.now()
      }.toFeatureFlag()
    }
  }

  fun delete(code: String): Boolean {
    return transaction(database) {
      val flag = FeatureFlagEntity.find { FeatureFlagTable.code eq code }.singleOrNull() ?: return@transaction false
      flag.deletedAt = Instant.now()
      true
    }
  }

  fun getByCode(code: String): FeatureFlag? {
    return transaction(database) {
      FeatureFlagEntity.find { (FeatureFlagTable.code eq code) }.find {
        it.deletedAt == null
      }?.toFeatureFlag()
    }
  }
}
