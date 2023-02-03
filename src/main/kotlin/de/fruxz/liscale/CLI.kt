package de.fruxz.liscale

import de.fruxz.liscale.data.config.Configuration
import de.fruxz.liscale.data.database.UserDataController
import de.fruxz.liscale.data.database.UserTable
import de.fruxz.liscale.extensions.task
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.system.exitProcess

object CLI {

	private val commands by lazy {
		this::class.functions.filter { it.hasAnnotation<Command>() }
	}

	fun launch(): Job = task {

		println("""
			Enter 'quit' to leave, or 'help' to receive a list of commands
		""".trimIndent())

		while (server.application.isActive) {
			print("$ ")

			readlnOrNull()?.let { rawInput ->
				commands.firstOrNull { it.name.equals(rawInput, true) }?.let { command ->
					println(message = "${command.call(CLI)}".trimIndent())
				}
			}

		}

	}

	@Command("quit")
	@Description("Disables the server and shuts this application completely down")
	fun quit() {
		println("Have a nice day!")
		exitProcess(0)
	}

	@Command("help")
	@Description("Receive a list of available commands to use")
	fun help() = buildString {
		appendLine("These are the available commands:")

		commands.forEach { command ->
			appendLine("> ${command.name}" + (command.findAnnotation<Description>()?.let { " * ${it.description}" } ?: ""))
		}

	}

	@Command("disableServer")
	@Description("If there is a problem, you can quickly disable the server with this")
	fun disconnectServer(): String {
		return if (server.application.isActive) {
			server.stop()
			"The server got successfully disabled!"
		} else "The server is currently not running!"
	}

	@Command("enableServer")
	@Description("If the server is offline, you can re-enable it with this")
	fun enableServer(): String {
		return if (server.application.isActive) {
			server = embeddedServer(Netty, port = Configuration.port, host = Configuration.host, module = Application::module).start()
			"The server got successfully enabled!"
		} else "The server is already running!"
	}

	@Command("createUser")
	@Description("Creates a new user without any permission, requires a name and a password")
	fun createUser(name: String, key: String) {
		UserDataController.createUser(name, key, emptyList())
	}

	@Command("removeUser")
	@Description("Deletes a user by its name")
	fun removeUser(name: String) {
		UserDataController.deleteUser(name)
	}

	@Command("addPermission")
	@Description("Adds the permission/product to a user by its name, use * for root access")
	fun addPermission(name: String, permission: String) {
		UserDataController.setUserProducts(name, (UserDataController.listUserProducts(name) ?: emptyList()) + permission)
	}

	@Command("removePermission")
	@Description("Takes a permission/product from a user by its name")
	fun removePermission(name: String, permission: String) {
		UserDataController.setUserProducts(name, (UserDataController.listUserProducts(name) ?: emptyList()) - permission)
	}

	@Command("test-function")
	fun test(name: String) = "Hello $name"

	private annotation class Command(val label: String)
	private annotation class Description(val description: String)

}