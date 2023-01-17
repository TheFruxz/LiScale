package de.fruxz.liscale.data.database

import de.fruxz.ascend.tool.json.dynamicJson
import org.jetbrains.exposed.sql.Table

object UserTable : Table("users") {
	val id = integer("id").autoIncrement()
	val username = varchar("username", 255)
	val password = varchar("password", 255)

	val assignedProducts = dynamicJson<List<String>>("assigned").nullable().default(null)

	override val primaryKey = PrimaryKey(id)
}