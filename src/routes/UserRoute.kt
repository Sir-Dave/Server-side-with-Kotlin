package com.sirdave.routes

import com.sirdave.API_VERSION
import com.sirdave.auth.JwtService
import com.sirdave.auth.MySession
import com.sirdave.repository.Repository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Route
import io.ktor.sessions.sessions
import io.ktor.sessions.set

const val USERS = "$API_VERSION/users"
const val USER_LOGIN = "$USERS/login"
const val USER_CREATE = "$USERS/create"

@KtorExperimentalLocationsAPI
@Location(USER_LOGIN)
class UserLoginRoute

@KtorExperimentalLocationsAPI
@Location(USER_CREATE)
class UserCreateRoute

@KtorExperimentalLocationsAPI
fun Route.users(db: Repository, jwtService: JwtService, hashFunction: (String) -> String) {
    post<UserCreateRoute> { // 2
        val signupParameters = call.receive<Parameters>() // 3
        val password = signupParameters["password"] // 4
                ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, "Missing Fields")
        val displayName = signupParameters["displayName"]
                ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, "Missing Fields")
        val email = signupParameters["email"]
                ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, "Missing Fields")
        val hash = hashFunction(password) // 5
        try {
            val newUser = db.addUser(email, displayName, hash) // 6
            newUser?.userId?.let {
                call.sessions.set(MySession(it))
                call.respondText(
                        jwtService.generateToken(newUser),
                        status = HttpStatusCode.Created
                )
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems creating User")
        }
    }

    post<UserLoginRoute> { // 1
        val signinParameters = call.receive<Parameters>()
        val password = signinParameters["password"]
                ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, "Missing Fields")
        val email = signinParameters["email"]
                ?: return@post call.respond(
                        HttpStatusCode.Unauthorized, "Missing Fields")
        val hash = hashFunction(password)
        try {
            val currentUser = db.findUserByEmail(email) // 2
            currentUser?.userId?.let {
                if (currentUser.passwordHash == hash) { // 3
                    call.sessions.set(MySession(it)) // 4
                    call.respondText(jwtService.generateToken(currentUser)) // 5
                } else {
                    call.respond(
                            HttpStatusCode.BadRequest, "Problems retrieving User") // 6
                }
            }
        } catch (e: Throwable) {
            application.log.error("Failed to register user", e)
            call.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        }
    }

}