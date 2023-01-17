package de.fruxz.liscale.plugins

import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.server.sessions.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureSecurity() {

	authentication {
		basic(name = "myauth1") {
			realm = "Ktor Server"
			validate { credentials ->
				if (credentials.name == credentials.password) {
					UserIdPrincipal(credentials.name)
				} else {
					null
				}
			}
		}

		form(name = "myauth2") {
			userParamName = "user"
			passwordParamName = "password"
			challenge {
				/**/
			}
		}
	}
	authentication {
		val myRealm = "MyRealm"
		val usersInMyRealmToHA1: Map<String, ByteArray> = mapOf(
			// pass="test", HA1=MD5("test:MyRealm:pass")="fb12475e62dedc5c2744d98eb73b8877"
			"test" to hex("fb12475e62dedc5c2744d98eb73b8877")
		)

		digest("myDigestAuth") {
			digestProvider { userName, realm ->
				usersInMyRealmToHA1[userName]
			}
		}
	}
	data class MySession(val count: Int = 0)
	install(Sessions) {
		cookie<MySession>("MY_SESSION") {
			cookie.extensions["SameSite"] = "lax"
		}
	}
	routing {
		authenticate("myauth1") {
			get("/protected/route/basic") {
				val principal = call.principal<UserIdPrincipal>()!!
				call.respondText("Hello ${principal.name}")
			}
		}
		authenticate("myauth2") {
			get("/protected/route/form") {
				val principal = call.principal<UserIdPrincipal>()!!
				call.respondText("Hello ${principal.name}")
			}
		}
		authenticate("myDigestAuth") {
			get("/protected/route/digest") {
				val principal = call.principal<UserIdPrincipal>()!!
				call.respondText("Hello ${principal.name}")
			}
		}
		get("/session/increment") {
			val session = call.sessions.get<MySession>() ?: MySession()
			call.sessions.set(session.copy(count = session.count + 1))
			call.respondText("Counter is ${session.count}. Refresh to increment.")
		}
	}
}
