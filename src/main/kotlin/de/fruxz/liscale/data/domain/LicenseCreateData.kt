package de.fruxz.liscale.data.domain

import de.fruxz.ascend.tool.timing.calendar.Calendar
import de.fruxz.liscale.data.domain.License.Limit
import kotlinx.serialization.Serializable

@Serializable
data class LicenseCreateData(
	val product: String,
	val expiration: Calendar? = null,
	val limits: List<Limit>? = null,
)
