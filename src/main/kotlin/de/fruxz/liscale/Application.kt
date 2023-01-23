package de.fruxz.liscale

import de.fruxz.ascend.extension.data.addAscendJsonModuleModification
import de.fruxz.ascend.extension.data.jsonBase
import de.fruxz.liscale.api.V1
import de.fruxz.liscale.data.config.Configuration
import de.fruxz.liscale.data.domain.License
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

fun main() {
	embeddedServer(Netty, port = Configuration.port, host = Configuration.host, module = Application::module)
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

	with(V1) {
		administration()
		licenseManagement()
	}

}
