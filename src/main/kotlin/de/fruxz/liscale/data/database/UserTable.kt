package de.fruxz.liscale.data.database

import de.fruxz.ascend.tool.json.dynamicJson
import de.fruxz.liscale.data.config.Configuration
import org.jetbrains.exposed.sql.Table

object UserTable : Table(Configuration.tableFormat.replace("%", "users")) {
	val id = integer("id").autoIncrement()
	val username = varchar("username", 255)
	val password = varchar("password", 255)

	val assignedProducts = dynamicJson<List<String>>("assigned").nullable().default(null)

	override val primaryKey = PrimaryKey(id)
}