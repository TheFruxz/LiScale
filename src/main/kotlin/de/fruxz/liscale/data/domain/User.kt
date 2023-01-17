package de.fruxz.liscale.data.domain

import io.ktor.server.auth.*

data class User(
	val id: Int,
	val username: String,
	val assignedProducts: List<String>?,
) : Principal {

	fun canAccessProduct(productId: String): Boolean {
		return assignedProducts?.any { it == "*" || it == productId } ?: false
	}

}
