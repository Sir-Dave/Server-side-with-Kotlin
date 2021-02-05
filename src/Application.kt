package com.sirdave

import com.sirdave.auth.JwtService
import com.sirdave.auth.MySession
import com.sirdave.auth.hash
import com.sirdave.repository.TodoRepository
import com.sirdave.routes.todos
import com.sirdave.routes.users
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.sessions.*
import io.ktor.features.*
import io.ktor.auth.*
import io.ktor.auth.jwt.jwt
import io.ktor.gson.*
import io.ktor.util.KtorExperimentalAPI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Locations) {
    }

    install(Sessions) {
        cookie<MySession>("MY_SESSION") {
            cookie.extensions["SameSite"] = "lax"
        }
    }

    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.Authorization)
        header("MyCustomHeader")
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    DatabaseFactory.connect()//init()
    val db = TodoRepository()
    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }

    install(Authentication) {
        // 1

        jwt("jwt") { //1
            verifier(jwtService.verifier) // 2
            realm = "Todo Server"
            validate { // 3
                val payload = it.payload
                val claim = payload.getClaim("id")
                val claimString = claim.asInt()
                val user = db.findUser(claimString) // 4
                user
            }
        }

    }

    install(ContentNegotiation) {
        gson {
        }
    }
    routing {
        get("/") {
            call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
        }
        users(db, jwtService, hashFunction)
        todos(db)


    }
}
const val API_VERSION = "/v1"

data class MySession(val count: Int = 0)

