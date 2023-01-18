package de.fruxz.liscale.data.database

import de.fruxz.ascend.tool.json.dynamicJson
import de.fruxz.ascend.tool.timing.calendar.calendar
import de.fruxz.liscale.data.config.Configuration
import de.fruxz.liscale.data.domain.License.*
import org.jetbrains.exposed.sql.Table
import java.lang.Integer.min
import kotlin.math.max

object LicenseTable : Table("licenses") {

	// Data
	val id = integer("id").autoIncrement()
	val product = varchar("product", 64)
	val key = varchar("key", 64)
	val status = enumeration<Status>("status").default(Status.ACTIVE)
	val label = dynamicJson<Label>("label").default(Label.UNTITLED)

	// Limitations
	val expiration = calendar("expiration").nullable()
	val limits = dynamicJson<List<Limit>>("limits").nullable().default(null)

	// Statistics
	val created = calendar("created")
	val sessions = integer("active-sessions").default(0) // dynamic
	val usages = long("usages").default(0) // dynamic

	override val primaryKey = PrimaryKey(id)
}