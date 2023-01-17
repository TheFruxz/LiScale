package de.fruxz.liscale

import de.fruxz.ascend.extension.data.addAscendJsonModuleModification
import de.fruxz.ascend.extension.data.jsonBase
import de.fruxz.liscale.api.V1
import de.fruxz.liscale.data.domain.License
import de.fruxz.liscale.plugins.configureRouting
import de.fruxz.liscale.plugins.configureSecurity
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun main() {
	embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
		.start(wait = true)
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

	configureSecurity()
	configureRouting()

	with(V1) {
		administration()
		licenseManagement()
	}

}
