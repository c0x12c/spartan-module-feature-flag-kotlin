package com.c0x12c.featureflag.repository

import com.c0x12c.featureflag.entity.FeatureFlag
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.UUID

class FeatureFlagRepository(
    private val jdbcUrl: String? = null,
    private val username: String? = null,
    private val password: String? = null
) {
    private val connection: Connection = DriverManager.getConnection(jdbcUrl, username, password)

    // Insert method for creating a new feature flag
    suspend fun insert(flag: FeatureFlag): UUID {
        val sql = """
            INSERT INTO feature_flags (id, name, code, description, enabled, metadata, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setObject(1, flag.id)
            statement.setString(2, flag.name)
            statement.setString(3, flag.code)
            statement.setString(4, flag.description)
            statement.setBoolean(5, flag.enabled)
            statement.setObject(6, flag.metadata)
            statement.setTimestamp(7, java.sql.Timestamp.from(flag.createdAt))
            statement.executeUpdate()
        }

        return flag.id
    }

    // Get a feature flag by its ID
    suspend fun getById(id: UUID): FeatureFlag? {
        val sql = "SELECT * FROM feature_flags WHERE id = ?"
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setObject(1, id)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return FeatureFlag(
                    id = UUID.fromString(resultSet.getString("id")),
                    name = resultSet.getString("name"),
                    code = resultSet.getString("code"),
                    description = resultSet.getString("description"),
                    enabled = resultSet.getBoolean("enabled"),
                    metadata = resultSet.getObject("metadata") as Map<String, Any>?,
                    createdAt = resultSet.getTimestamp("created_at").toInstant(),
                    updatedAt = resultSet.getTimestamp("updated_at")?.toInstant()
                )
            }
        }
        return null
    }

    // Get a feature flag by its code
    suspend fun getByCode(code: String): FeatureFlag? {
        val sql = "SELECT * FROM feature_flags WHERE code = ?"
        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, code)
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return FeatureFlag(
                    id = UUID.fromString(resultSet.getString("id")),
                    name = resultSet.getString("name"),
                    code = resultSet.getString("code"),
                    description = resultSet.getString("description"),
                    enabled = resultSet.getBoolean("enabled"),
                    metadata = resultSet.getObject("metadata") as Map<String, Any>?,
                    createdAt = resultSet.getTimestamp("created_at").toInstant(),
                    updatedAt = resultSet.getTimestamp("updated_at")?.toInstant()
                )
            }
        }
        return null
    }

    // List feature flags with pagination (limit and skip)
    suspend fun list(limit: Int, skip: Int): List<FeatureFlag> {
        val sql = "SELECT * FROM feature_flags LIMIT ? OFFSET ?"
        val flags = mutableListOf<FeatureFlag>()
        connection.prepareStatement(sql).use { statement ->
            statement.setInt(1, limit)
            statement.setInt(2, skip)
            val resultSet = statement.executeQuery()

            while (resultSet.next()) {
                flags.add(
                    FeatureFlag(
                        id = UUID.fromString(resultSet.getString("id")),
                        name = resultSet.getString("name"),
                        code = resultSet.getString("code"),
                        description = resultSet.getString("description"),
                        enabled = resultSet.getBoolean("enabled"),
                        metadata = resultSet.getObject("metadata") as Map<String, Any>?,
                        createdAt = resultSet.getTimestamp("created_at").toInstant(),
                        updatedAt = resultSet.getTimestamp("updated_at")?.toInstant()
                    )
                )
            }
        }
        return flags
    }

    // Update an existing feature flag
    suspend fun update(flag: FeatureFlag) {
        val sql = """
            UPDATE feature_flags
            SET name = ?, description = ?, enabled = ?, metadata = ?, updated_at = ?
            WHERE code = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setString(1, flag.name)
            statement.setString(2, flag.description)
            statement.setBoolean(3, flag.enabled)
            statement.setObject(4, flag.metadata)
            statement.setTimestamp(5, java.sql.Timestamp.from(flag.updatedAt ?: java.time.Instant.now()))
            statement.setString(6, flag.code)
            statement.executeUpdate()
        }
    }

    // Delete a feature flag by its ID
    suspend fun delete(id: UUID) {
        val sql = "DELETE FROM feature_flags WHERE id = ?"
        connection.prepareStatement(sql).use { statement: PreparedStatement ->
            statement.setObject(1, id)
            statement.executeUpdate()
        }
    }
}
