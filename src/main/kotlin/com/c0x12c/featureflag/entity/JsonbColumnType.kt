import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.postgresql.util.PGobject
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class JsonbColumnType : ColumnType() {
  private val objectMapper = jacksonObjectMapper()

  override fun sqlType(): String = "JSONB"

  override fun valueFromDB(value: Any): Any = when (value) {
    is PGobject -> value.value ?: "{}"
    is String -> value
    else -> "{}"
  }

  override fun valueToDB(value: Any?): Any? = when (value) {
    is String -> PGobject().apply {
      type = "jsonb"
      this.value = value
    }
    is Map<*, *> -> PGobject().apply {
      type = "jsonb"
      this.value = objectMapper.writeValueAsString(value)
    }
    else -> null
  }

  override fun notNullValueToDB(value: Any): Any = valueToDB(value) ?: PGobject().apply {
    type = "jsonb"
    this.value = "{}"
  }
}

fun Table.jsonb(name: String): Column<String> = registerColumn(name, JsonbColumnType())