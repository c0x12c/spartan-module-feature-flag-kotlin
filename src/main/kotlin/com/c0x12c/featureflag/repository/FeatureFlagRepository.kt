package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlag
import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.models.MetadataContent
import com.c0x12c.featureflag.table.FeatureFlagTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class FeatureFlagRepository(private val database: Database) {

  private val json = Json { ignoreUnknownKeys = true }

  fun insert(featureFlag: FeatureFlag): UUID {
    return transaction(database) {
      FeatureFlagEntity.new {
        name = featureFlag.name
        code = featureFlag.code
        description = featureFlag.description
        enabled = featureFlag.enabled
        metadata = featureFlag.metadata?.let { json.encodeToString(it) }
        type = featureFlag.metadata?.let { it::class.simpleName }
        createdAt = Instant.now()
      }.id.value
    }
  }

  fun getById(id: UUID): FeatureFlag? {
    return transaction(database) {
      FeatureFlagEntity.findById(id)?.let {
        if (it.deletedAt == null) it.toFeatureFlag() else null
      }
    }
  }

  fun getByCode(code: String): FeatureFlag? {
    return transaction(database) {
      FeatureFlagEntity.find {
        (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt.isNull())
      }.firstOrNull()?.toFeatureFlag()
    }
  }

  fun updateEnableStatus(code: String, enabled: Boolean): FeatureFlag? {
    return transaction(database) {
      val entity = FeatureFlagEntity.find {
        (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt.isNull())
      }.firstOrNull() ?: return@transaction null

      entity.apply {
        this.enabled = enabled
        updatedAt = Instant.now()
      }.toFeatureFlag()
    }
  }

  fun update(code: String, featureFlag: FeatureFlag): FeatureFlag? {
    return transaction(database) {
      val entity = FeatureFlagEntity.find {
        (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt.isNull())
      }.firstOrNull() ?: return@transaction null
      entity.apply {
        name = featureFlag.name
        description = featureFlag.description
        enabled = featureFlag.enabled
        metadata = featureFlag.metadata?.let { json.encodeToString(it) }
        type = featureFlag.metadata?.let { it::class.simpleName }
        updatedAt = Instant.now()
      }.toFeatureFlag()
    }
  }

  fun delete(code: String): FeatureFlag? {
    return transaction(database) {
      val entity = FeatureFlagEntity.find {
        (FeatureFlagTable.code eq code) and (FeatureFlagTable.deletedAt.isNull())
      }.firstOrNull() ?: return@transaction null

      entity.deletedAt = Instant.now()
      entity.toFeatureFlag()
    }
  }

  fun list(limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return transaction(database) {
      FeatureFlagEntity.find { FeatureFlagTable.deletedAt.isNull() }
        .limit(limit, offset.toLong())
        .map { it.toFeatureFlag() }
    }
  }

  fun findByMetadataType(type: String, limit: Int = 100, offset: Int = 0): List<FeatureFlag> {
    return transaction(database) {
      FeatureFlagEntity.find {
        (FeatureFlagTable.type eq type) and (FeatureFlagTable.deletedAt.isNull())
      }
        .limit(limit, offset.toLong())
        .map { it.toFeatureFlag() }
    }
  }

  private fun FeatureFlagEntity.toFeatureFlag(): FeatureFlag {
    return FeatureFlag(
      id = id.value,
      name = name,
      code = code,
      description = description,
      enabled = enabled,
      metadata = metadata?.let { json.decodeFromString<MetadataContent>(it) },
      createdAt = createdAt,
      updatedAt = updatedAt,
      deletedAt = deletedAt
    )
  }
}
