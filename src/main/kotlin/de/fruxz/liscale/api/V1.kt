package de.fruxz.liscale.api

import de.fruxz.ascend.extension.data.jsonBase
import de.fruxz.ascend.extension.data.toJsonString
import de.fruxz.ascend.extension.tryOrNull
import de.fruxz.liscale.data.database.LicenseDataController
import de.fruxz.liscale.data.database.UserDataController
import de.fruxz.liscale.data.domain.LicenseActivationRespond
import de.fruxz.liscale.data.domain.LicenseActivationRespond.DenyReason.NOT_FOUND
import de.fruxz.liscale.data.domain.LicenseData
import de.fruxz.liscale.data.domain.User
import de.fruxz.liscale.data.domain.User.UserData
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

			authenticate("basic") {

				get("v1/license/info") {
					val executor = call.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@get
					val (product, key) = (input["product"] ?: return@get) to (input["license"] ?: return@get)

					if (executor?.canAccessProduct(product) == true) {
						call.respond(Companion.OK, LicenseDataController.getLicense(product, key).toJsonString())
					} else call.respond(HttpStatusCode.Forbidden, "You are not allowed to view licenses inside this product.")

				}

				post("v1/license/create") {
					val executor = call.principal<User>()
					val input = tryOrNull { call.receive<LicenseData>() } ?: return@post

					if (executor?.canAccessProduct(input.product) == true) {
						tryOrNull {
							val process = tryOrNull(false) {
								with(input) {
									LicenseDataController.createLicense(
										product,
										expiration,
										limits
									)
								}
							}

							if (process != null) {

								call.respond(
									status = HttpStatusCode.Created,
									message = process.toJsonString()
								)

							} else call.respond(HttpStatusCode.BadRequest, "License creation failed.")

						} ?: call.respond(HttpStatusCode.InternalServerError, "Something went wrong")
					} else call.respond(HttpStatusCode.Forbidden, "You are not allowed to create licenses for this product.")
				}

				patch("v1/license/revoke") {
					val executor = call.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@patch
					val (product, license) = (input["product"] ?: return@patch) to (input["license"] ?: return@patch)

					if (executor?.canAccessProduct(product) == true) {
						when (LicenseDataController.revokeLicense(product, license)) {
							true -> call.respond(HttpStatusCode.OK)
							false -> call.respond(
								HttpStatusCode.NotAcceptable,
								"License not found / could not be revoked!"
							)
						}
					} else call.respond(HttpStatusCode.Forbidden, "You are not allowed to revoke licenses for this product.")

				}

				delete("v1/license/delete") {
					val executor = call.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() } ?: return@delete
					val (product, license) = (input["product"] ?: return@delete) to (input["license"] ?: return@delete)

					if (executor?.canAccessProduct(product) == true) {
						when (LicenseDataController.deleteLicense(product, license)) {
							true -> call.respond(HttpStatusCode.OK)
							false -> call.respond(
								HttpStatusCode.NotAcceptable,
								"License not found / could not be deleted!"
							)
						}
					} else call.respond(HttpStatusCode.Forbidden, "You are not allowed to delete licenses for this product.")

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

				UserDataController.createUser(username, password, listOf("*"))

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
							} else call.respond(HttpStatusCode.NotAcceptable, "No product specified.")
						} else call.respond(HttpStatusCode.BadRequest, "Json body is missing!")
					} else call.respond(HttpStatusCode.Forbidden)

				}

				post("v1/admin/user/create") {
					val executor = context.principal<User>()
					val input = tryOrNull { call.receive<UserData>() }

					if (executor != null && executor.isRoot()) {
						if (input != null) {
							val user = UserDataController.getUser(input.username)

							if (user == null) {

								when (UserDataController.createUser(input.username, input.password, input.assignedProducts)) {
									true -> call.respond(HttpStatusCode.Created)
									false -> call.respond(HttpStatusCode.NotAcceptable, "User could not be created.")
								}

							} else call.respond(HttpStatusCode.Conflict, "User already exists!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to create users. (ROOT/* does)")

				}

				delete("v1/admin/user/delete") {
					val executor = context.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (executor != null && executor.isRoot()) {
						if (input != null) {
							val user = input["username"]?.let { it1 -> UserDataController.getUser(it1) }

							if (user != null) {

								when (UserDataController.deleteUser(user.username)) {
									true -> call.respond(HttpStatusCode.OK)
									false -> call.respond(
										HttpStatusCode.NotAcceptable,
										"User could not be deleted."
									)
								}

							} else call.respond(HttpStatusCode.NotFound, "User does not exist!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to delete users. (ROOT/* does)")
				}

				get("v1/admin/user/products") {
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (input != null) {
						val user = input["username"]?.let { it1 -> UserDataController.getUser(it1) }

						if (user != null) {

							call.respond(Companion.OK, user.assignedProducts.toJsonString())

						} else call.respond(HttpStatusCode.NotFound, "User does not exist!")

					} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")

				}

				put("v1/admin/user/products") {
					val executor = context.principal<User>()
					val input = tryOrNull(false) { call.receive<JsonElement>() }

					if (executor != null && executor.isRoot()) {
						if (input != null) {
							val user = input.jsonObject["username"]?.jsonPrimitive?.let { it1 -> UserDataController.getUser(it1.content) }

							if (user != null) {

								val products = input.jsonObject["products"]?.jsonArray?.map { it.jsonPrimitive.content }

								if (products != null) {

									when (UserDataController.setUserProducts(user.username, products)) {
										true -> call.respond(HttpStatusCode.OK)
										false -> call.respond(
											HttpStatusCode.NotAcceptable,
											"User products could not be updated."
										)
									}

								} else call.respond(Companion.NotAcceptable, "No products specified.")

							} else call.respond(HttpStatusCode.NotFound, "User does not exist!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to change user product assignments. (ROOT/* does)")
				}

				patch("v1/admin/user/password") {
					val executor = context.principal<User>()
					val input = tryOrNull { jsonBase.parseToJsonElement(call.receiveText()) }

					if (executor != null && executor.isRoot()) {
						if (input != null) {
							val user = input.jsonObject["username"]?.jsonPrimitive?.let { it1 -> UserDataController.getUser(it1.content) }

							if (user != null) {

								val password = input.jsonObject["password"]?.jsonPrimitive?.content

								if (password != null) {

									when (UserDataController.updateUserPassword(user.username, password)) {
										true -> call.respond(HttpStatusCode.OK)
										false -> call.respond(
											HttpStatusCode.NotAcceptable,
											"User password could not be updated."
										)
									}

								} else call.respond(Companion.NotAcceptable, "No password specified.")

							} else call.respond(HttpStatusCode.NotFound, "User does not exist!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to change user passwords. (ROOT/* does)")
				}

				get("v1/admin/user/check") {
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (input != null) {
						val user = tryOrNull { UserDataController.getUser(input["username"]!!, input["password"]!!) }

						if (user != null) {

							call.respond(Companion.OK, user.toJsonString())

						} else call.respond(HttpStatusCode.NotFound, "User does not exist or password is incorrect!")

					} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")

				}

				get("v1/admin/user/list") {
					val executor = context.principal<User>()

					if (executor != null && executor.isRoot()) {
						call.respond(Companion.OK, UserDataController.listUsers().toJsonString())
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to list users. (ROOT/* does)")

				}

				get("v1/admin/user/view") {
					val executor = context.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (executor != null && executor.isRoot()) {
						if (input != null) {
							val user = input["username"]?.let { it1 -> UserDataController.getUser(it1) }

							if (user != null) {

								call.respond(Companion.OK, user.toJsonString())

							} else call.respond(HttpStatusCode.NotFound, "User does not exist!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to view users. (ROOT/* does)")
				}

				get("v1/admin/product") {
					val executor = context.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (executor?.isRoot() == true) {
						if (input != null) {
							val category = input["product"]?.let { it1 -> LicenseDataController.productInfo(it1) }

							if (category != null) {
								call.respond(Companion.OK, category.toJsonString())
							} else call.respond(HttpStatusCode.NotFound, "Product does not exist!")

						}
					}

				}

				delete("v1/admin/product") {
					val executor = context.principal<User>()
					val input = tryOrNull { call.receive<Map<String, String>>() }

					if (executor?.isRoot() == true) {
						if (input != null) {
							val product = input["product"]

							if (product != null) {
								if (LicenseDataController.deleteProduct(product)) {
									call.respond(Companion.OK)
								} else call.respond(HttpStatusCode.NotModified, "Nothing deleted, product may does not exists?")
							} else call.respond(HttpStatusCode.NotFound, "Product does not exist!")

						} else call.respond(HttpStatusCode.NotAcceptable, "Json body is missing or invalid user data!")
					} else call.respond(HttpStatusCode.Forbidden, "You do not have the permission to delete products. (ROOT/* does)")
				}

			}
		}
	}

}