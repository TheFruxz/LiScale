package de.fruxz.liscale.data.domain

import de.fruxz.ascend.extension.tryOrNull
import de.fruxz.ascend.tool.timing.calendar.Calendar
import de.fruxz.liscale.data.database.LicenseDataController
import de.fruxz.liscale.data.database.LicenseTable
import de.fruxz.liscale.data.database.LicenseTable.product
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select

@Serializable
data class License(
	val id: Int,
	val product: String,
	val key: String,
	val status: Status,
	val label: Label?,

	val expiration: Calendar?,
	val limits: List<Limit>?,

	val created: Calendar,
) {

	@Serializable
	data class Label(
		val title: String,
		val description: String,
		val tags: List<String>,
	) {

		companion object {

			val UNTITLED = Label(
				title = "Untitled",
				description = "",
				tags = emptyList(),
			)

		}

	}

	enum class Status {
		ACTIVE,
		REVOKED;
	}

	interface Limit {

		val denyRespond: DenyReason
		suspend fun isAchieved(product: String, key: String): Boolean

		@Serializable
		data class ActiveSessionLimit(val maxSessions: Int) : Limit {
			override val denyRespond = TOO_MANY_ACTIVE_SESSIONS
			override suspend fun isAchieved(product: String, key: String): Boolean {
				return tryOrNull {
					LicenseTable
						.select { LicenseTable.product eq product and (LicenseTable.key eq key) }
						.first()[LicenseTable.sessions] <= maxSessions
				} ?: false
			}
		}

		@Serializable
		data class UsedAmountsLimit(val maxUses: Int) : Limit {
			override val denyRespond = LIMIT_REACHED
			override suspend fun isAchieved(product: String, key: String): Boolean {
				return tryOrNull {
					LicenseTable
						.select { LicenseTable.product eq product and (LicenseTable.key eq key) }
						.first()[LicenseTable.usages] <= maxUses
				} ?: false
			}
		}

	}

}