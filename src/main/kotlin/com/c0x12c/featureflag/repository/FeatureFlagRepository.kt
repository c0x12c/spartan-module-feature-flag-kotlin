package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlagEntity
import com.c0x12c.featureflag.table.FeatureFlagTable
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

class FeatureFlagRepository(private val database: Database) {
  private val objectMapper = jacksonObjectMapper()

  fun insert(flagData: Map<String, Any>): UUID {
    return transaction(database) {
      FeatureFlagEntity.new {
        name = flagData["name"] as String
        code = flagData["code"] as String
        description = flagData["description"] as? String ?: ""
        enabled = flagData["enabled"] as? Boolean ?: false
        metadata = objectMapper.writeValueAsString(flagData["metadata"] ?: emptyMap<String, Any>())
        createdAt = Instant.now()
      }.id.value
    }
  }

  fun getById(id: UUID): Map<String, Any>? {
    return transaction(database) {
      FeatureFlagEntity.findById(id)?.toMap()
    }
  }

  fun list(limit: Int = 100, offset: Int = 0): List<Map<String, Any>> {
    return transaction(database) {
      FeatureFlagEntity.all().limit(limit, offset.toLong()).map { it.toMap() }
    }
  }

  fun update(code: String, flagData: Map<String, Any>): Map<String, Any>? {
    return transaction(database) {
      val flag = FeatureFlagEntity.find { FeatureFlagTable.code eq code }.singleOrNull() ?: return@transaction null

      flag.apply {
        name = flagData["name"] as? String ?: name
        description = flagData["description"] as? String ?: description ?: ""
        enabled = flagData["enabled"] as? Boolean ?: enabled
        metadata = if (flagData.containsKey("metadata")) {
          objectMapper.writeValueAsString(flagData["metadata"] ?: emptyMap<String, Any>())
        } else {
          metadata
        }
        updatedAt = Instant.now()
      }.toMap()
    }
  }

  fun delete(code: String): Boolean {
    return transaction(database) {
      val flag = FeatureFlagEntity.find { FeatureFlagTable.code eq code }.singleOrNull() ?: return@transaction false
      flag.deletedAt = Instant.now()
      true
    }
  }

  fun getByCode(code: String): Map<String, Any>? {
    return transaction(database) {
      FeatureFlagEntity.find { (FeatureFlagTable.code eq code) }.find {
        it.deletedAt == null
      }?.toMap()
    }
  }

  private fun FeatureFlagEntity.toMap(): Map<String, Any> {
    return mapOf(
      "id" to id.value,
      "name" to name,
      "code" to code,
      "description" to (description ?: ""),
      "enabled" to enabled,
      "metadata" to objectMapper.readValue(metadata, Map::class.java),
      "createdAt" to createdAt.toString(),
      "updatedAt" to (updatedAt?.toString() ?: ""),
      "deletedAt" to (deletedAt?.toString() ?: "")
    )
  }
}