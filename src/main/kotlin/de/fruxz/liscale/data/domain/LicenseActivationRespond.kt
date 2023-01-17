package de.fruxz.liscale.data.domain

import kotlinx.serialization.Serializable

@Serializable
data class LicenseActivationRespond(
	val success: Boolean,
	val reason: DenyReason?,
) {

	enum class DenyReason {
		NOT_FOUND,
		ACCESS_REVOKED,
		EXPIRED,
		LIMIT_REACHED,
		TOO_MANY_ACTIVE_SESSIONS;
	}

}
