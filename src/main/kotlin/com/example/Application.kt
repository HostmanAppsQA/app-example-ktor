package com.example

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.request.receive
import kotlinx.serialization.Serializable
import org.slf4j.event.Level
import java.util.concurrent.atomic.AtomicLong

fun main() {
    embeddedServer(Netty, port = 4000, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

@Serializable
data class HelloResponse(val message: String)

@Serializable
data class CreateUserRequest(val name: String)

@Serializable
data class User(val id: Long, val name: String)

@Serializable
data class ErrorResponse(val error: String)

private val userIdSeq = AtomicLong(0)
private val users = mutableListOf<User>()

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Unknown error"))
        }
    }

    routing {
        get("/") {
            call.respond(HelloResponse("Hello from Ktor on port 4000"))
        }

        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        get("/users") {
            call.respond(users)
        }

        post("/users") {
            val req = call.receive<CreateUserRequest>()
            val user = User(id = userIdSeq.incrementAndGet(), name = req.name)
            users += user
            call.respond(HttpStatusCode.Created, user)
        }
    }
}
