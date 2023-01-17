package de.fruxz.liscale.api

import de.fruxz.ascend.extension.data.toJsonString
import de.fruxz.ascend.extension.tryOrNull
import de.fruxz.liscale.data.database.LicenseDataController
import de.fruxz.liscale.data.database.UserDataController
import de.fruxz.liscale.data.domain.LicenseActivationRespond
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason.NOT_FOUND
import de.fruxz.liscale.data.domain.LicenseCreateData
import de.fruxz.liscale.data.domain.User
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

object V1 {

	fun Application.licenseManagement() {
		routing {

			get("v1/license/status") {
				val input = tryOrNull { call.receive<Map<String, String>>() }
				val (product, key) = input?.get("product") to input?.get("license")

				if (input == null) { call.respond(HttpStatusCode.NotAcceptable, "input not valid json"); return@get }
				if (product == null) { call.respond(HttpStatusCode.NotAcceptable, "'product' not valid"); return@get }
				if (key == null) { call.respond(HttpStatusCode.NotAcceptable, "'license' not valid"); return@get }

				when (val response = LicenseDataController.licenseDenyStatus(product, key).await()) {
					null -> call.respond(HttpStatusCode.OK, "usable")
					NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.name)
					else -> call.respond(HttpStatusCode.Forbidden, response.name)
				}

			}

			get("v1/license/info") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@get
				val (product, key) = (input["product"] ?: return@get) to (input["license"] ?: return@get)

				call.respond(Companion.OK, LicenseDataController.getLicense(product, key).toJsonString())

			}

			post("v1/license/create") {
				val input = tryOrNull { call.receive<LicenseCreateData>() } ?: return@post

				tryOrNull {
					val process = tryOrNull(false) { with(input) { LicenseDataController.createLicense(product, expiration, limits) } }

					if (process != null) {

						call.respond(
							HttpStatusCode.Created,
							process.toJsonString()
						)

					} else call.respond(HttpStatusCode.BadRequest).also { println("License creation failed") }

				} ?: call.respond(HttpStatusCode.InternalServerError).also { println("License creation failed.") }

			}

			patch("v1/license/activate") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@patch
				val (product, license) = (input["product"] ?: return@patch) to (input["license"] ?: return@patch)

				call.respond(
					LicenseDataController.activateLicense(product, license).await().let { result ->
						call.respond(
							when (result) {
								NOT_FOUND -> HttpStatusCode.NotAcceptable
								null -> HttpStatusCode.OK
								else -> HttpStatusCode.Forbidden
							},
							LicenseActivationRespond(
								result == null,
								result,
							)
						)
					}
				)

			}

			patch("v1/license/disable") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@patch
				val (product, license) = (input["product"] ?: return@patch) to (input["license"] ?: return@patch)

				call.respond(
					when (LicenseDataController.disableLicense(product, license)) {
						true -> call.respond(HttpStatusCode.OK)
						false -> call.respond(HttpStatusCode.NotAcceptable, "Nothing changed, wrong license or already disabled.")
					}
				)

			}

			patch("v1/license/revoke") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@patch
				val (product, license) = (input["product"] ?: return@patch) to (input["license"] ?: return@patch)

				when (LicenseDataController.revokeLicense(product, license)) {
					true -> call.respond(HttpStatusCode.OK)
					false -> call.respond(HttpStatusCode.NotAcceptable, "License not found / could not be revoked!")
				}

			}

			delete("v1/license/delete") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@delete
				val (product, license) = (input["product"] ?: return@delete) to (input["license"] ?: return@delete)

				when (LicenseDataController.deleteLicense(product, license)) {
					true -> call.respond(HttpStatusCode.OK)
					false -> call.respond(HttpStatusCode.NotAcceptable, "License not found / could not be deleted!")
				}

			}

		}
	}

	fun Application.administration() {

		authentication {
			basic(name = "basic") {
				realm = "LiScale Server"
				validate { credentials ->
					UserDataController.getUser(credentials.name, credentials.password)
				}
			}
		}

		routing {

			get("register-demo") {
				val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@get
				val (username, password) = (input["username"] ?: return@get) to (input["password"] ?: return@get)

				UserDataController.createUser(username, password, emptyList())

			}

			authenticate("basic") {

				get("v1/admin/product/list") {
					call.respond(Companion.OK, LicenseDataController.listProducts().toJsonString())
				}

				get("v1/admin/license/list") {
					val user = context.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() }
					val product = input?.get("product")

					if (user != null) {
						if (input != null) {
							if (product != null) {
								if (user.canAccessProduct(product)) {

									call.respond(Companion.OK, LicenseDataController.listLicenses(product).toJsonString())

								} else call.respond(HttpStatusCode.Forbidden, "You do not have access to this product.")
							} else call.respond(HttpStatusCode.BadRequest, "No product specified.")
						} else call.respond(HttpStatusCode.BadRequest, "Json body is missing!")
					} else call.respond(HttpStatusCode.Forbidden)

				}

			}
		}
	}

}