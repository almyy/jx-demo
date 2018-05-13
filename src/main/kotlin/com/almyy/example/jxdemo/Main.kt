package com.almyy.example.jxdemo

import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import khttp.post
import org.joda.time.DateTime
import org.json.JSONObject
import org.slf4j.LoggerFactory
import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    val menu: String by TimedUpdate(1, ::fetchMenu)
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            get("/ready") {
                call.respond(HttpStatusCode.OK, "")
            }

            get("/menu") {
                call.respondText(menu, ContentType.Application.Json)
            }
        }
    }
    server.start(wait = true)
}

fun fetchMenu(): String {
    val lte = "\$lte"
    val gte = "\$gte"
    val today = DateTime.now().toDateTimeISO()
    val tomorrow = DateTime.now().plusDays(1).toDateTimeISO()
    val json = """
        |{
        |   "where": {
        |       "date": {
        |           "$gte": {
        |                "__type": "Date",
        |               "iso": "$today"
        |           },
        |           "$lte": {
        |               "__type": "Date",
        |               "iso": "$tomorrow"
        |           }
        |       }
        |   },
        |   "_method": "get"
        |}
        """.trimMargin()
    return post(
            url = "http://lunch-menu.herokuapp.com/parse/classes/Menu",
            headers = mapOf("X-Parse-Application-Id" to "nAixMGyDvVeNfeWEectyJrvtqSeKregQs2gLh9Aw"),
            json = JSONObject(json)
    ).text
}

class TimedUpdate(private val days: Int, private val fetch: () -> String) {
    private var menu = fetch()
    private var lastUpdated = DateTime.now()
    private val logger = LoggerFactory.getLogger(this::class.java)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        if (menu == "" || lastUpdated.withTimeAtStartOfDay().plusDays(days).isBeforeNow) {
            logger.info("Fetching new menu")
            menu = fetch()
            lastUpdated = DateTime.now()
        }
        return menu
    }
}