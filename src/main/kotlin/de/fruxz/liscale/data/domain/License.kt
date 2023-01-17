package de.fruxz.liscale.data.domain

import de.fruxz.ascend.tool.timing.calendar.Calendar
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason.*
import kotlinx.serialization.Serializable

@Serializable
data class License(
	val id: Int,
	val product: String,
	val key: String,
	val status: Status,

	val expiration: Calendar?,
	val limits: List<Limit>?,

	val created: Calendar,
) {

	enum class Status {
		ACTIVE,
		REVOKED;
	}

	interface Limit {

		val denyRespond: DenyReason
		suspend fun isAchieved(): Boolean

		@Serializable
		data class ActiveSessionLimit(val maxSessions: Int) : Limit {
			override val denyRespond = TOO_MANY_ACTIVE_SESSIONS
			override suspend fun isAchieved(): Boolean {
				TODO("Not yet implemented")
			}
		}

		@Serializable
		data class UsedAmountsLimit(val maxUses: Int) : Limit {
			override val denyRespond = LIMIT_REACHED
			override suspend fun isAchieved(): Boolean {
				TODO("Not yet implemented")
			}
		}

	}

}