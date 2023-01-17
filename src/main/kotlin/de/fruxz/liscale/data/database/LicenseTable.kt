package de.fruxz.liscale.data.database

import de.fruxz.ascend.tool.json.dynamicJson
import de.fruxz.ascend.tool.timing.calendar.calendar
import de.fruxz.liscale.data.domain.License.*
import org.jetbrains.exposed.sql.Table

object LicenseTable : Table("licenses") {

	// Data
	val id = integer("id").autoIncrement()
	val product = varchar("product", 255)
	val key = varchar("key", 23)
	val status = enumeration<Status>("status").default(Status.ACTIVE)

	// Limitations
	val expiration = calendar("expiration").nullable()
	val limits = dynamicJson<List<Limit>>("limits").nullable().default(null)

	// Statistics
	val created = calendar("created")
	val sessions = integer("active-sessions").default(0) // dynamic
	val usages = long("usages").default(0) // dynamic

	override val primaryKey = PrimaryKey(id)
}