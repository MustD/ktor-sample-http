package com.example.plugins

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.auth.principal
import io.ktor.server.locations.Location
import io.ktor.server.locations.Locations
import io.ktor.server.locations.get
import io.ktor.server.plugins.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import java.time.LocalDateTime
import kotlin.collections.set

fun Application.configureRouting() {
    install(Locations) { }
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
            findAndRegisterModules()
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }
    data class MySession(val sugar: Int = 0, val buckwheat: Int = 0)
    install(Sessions) {
        cookie<MySession>("SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    authentication {
        basic(name = "base_auth") {
            realm = "Ktor Server"
            validate { credentials ->
                if (UserCreditStore.users.contains(credentials.name to credentials.password)) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }


    routing {
        get("/") {
            val response = object {
                val timestamp = LocalDateTime.now()
                val message = "Добро пожаловать любителям гречневого варенья"
            }
            call.respond(response)
        }
        authenticate("base_auth") {
            get("/limit") {
                val principal = call.principal<UserIdPrincipal>()!!
                when (principal.name) {
                    UserCreditStore.users.get(0).first -> call.respond(
                        object {
                            val sugar = -10
                            val buckwheat = -1
                        })
                    UserCreditStore.users.get(1).first -> call.respond(
                        object {
                            val sugar = 2
                            val buckwheat = 10
                        })
                }
            }
        }

        post("/sugar/add") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(sugar = session.sugar + 1))
            call.respondText("Sugar count is ${session.sugar + 1}.")
        }

        post("/buckwheat/add") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.sessions.set(session.copy(buckwheat = session.buckwheat + 1))
            call.respondText("Buckwheat count is ${session.buckwheat + 1}.")
        }

        get("/order") {
            val session = call.sessions.get<MySession>() ?: MySession()
            call.respond(object {
                val sugar = session.sugar
                val buckwheat = session.buckwheat
            })
        }

        post("/address") {
            data class Address(val location: String)

            val request = call.receive<Address>()
            call.respond(object {
                val timestamp = LocalDateTime.now()
                val message = request.location
            })
        }

        val states = mapOf(
            1 to "Samara",
            2 to "Moskow",
            3 to "Kalinigrad",
        )
        get("/states") {
            call.respond(states)
        }

        val state2cities = mapOf(
            1 to listOf("Samara", "Togliatty", "Izjevsk"),
            2 to listOf("Moskow"),
            3 to listOf("Kalinigrad", "Svetlogorsk"),
        )
        get("/cities/{stateId}") {
            val stateId = call.parameters["stateId"] ?: throw RuntimeException("unable to find")
            val cities = state2cities[stateId.toInt()] ?: throw RuntimeException("unable to find")
            call.respond(cities)
        }
    }
}
