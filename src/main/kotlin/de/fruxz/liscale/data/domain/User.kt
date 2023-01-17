package de.fruxz.liscale.data.domain

import io.ktor.server.auth.*
import kotlinx.serialization.Serializable

@Serializable
data class User(
	val id: Int,
	val username: String,
	val assignedProducts: List<String>?,
) : Principal {

	fun canAccessProduct(productId: String): Boolean {
		return assignedProducts?.any { it == "*" || it == productId } ?: false
	}

	fun isRoot() = assignedProducts?.contains("*") ?: false

	@Serializable
	data class UserData(
		val username: String,
		val password: String,
		val assignedProducts: List<String>? = null,
	)

}
