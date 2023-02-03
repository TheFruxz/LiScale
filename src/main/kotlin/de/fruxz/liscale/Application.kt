package de.fruxz.liscale

import de.fruxz.ascend.extension.data.addAscendJsonModuleModification
import de.fruxz.ascend.extension.data.jsonBase
import de.fruxz.ascend.extension.data.toJsonString
import de.fruxz.ascend.extension.future.await
import de.fruxz.liscale.api.V1
import de.fruxz.liscale.data.config.Configuration
import de.fruxz.liscale.data.database.LicenseTable
import de.fruxz.liscale.data.domain.License
import de.fruxz.liscale.extensions.smartTransaction
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.jetbrains.exposed.sql.selectAll

lateinit var server: NettyApplicationEngine

suspend fun main() {

	server = embeddedServer(Netty, port = Configuration.port, host = Configuration.host, module = Application::module).start()

	CLI.launch().await()

}

fun setupJSON() {

	addAscendJsonModuleModification {
		polymorphic(License.Limit::class) {
			subclass(License.Limit.ActiveSessionLimit::class)
			subclass(License.Limit.UsedAmountsLimit::class)
		}
	}

}

fun Application.module() {
	setupJSON()

	install(ContentNegotiation) {
		json(jsonBase)
	}

	with(V1) {
		administration()
		licenseManagement()
	}

}
