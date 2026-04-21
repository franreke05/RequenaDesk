package com.requena.supportdesk.server.application

import com.requena.supportdesk.server.config.resolveServerHost
import com.requena.supportdesk.server.config.resolveServerPort
import io.ktor.server.application.Application
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine

class SupportDeskServerHandle internal constructor(
    private val engine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>,
) {
    fun stop(gracePeriodMillis: Long = 1_000, timeoutMillis: Long = 5_000) {
        engine.stop(gracePeriodMillis, timeoutMillis)
    }
}

fun startSupportDeskServer(wait: Boolean = true): SupportDeskServerHandle {
    val engine = embeddedServer(
        Netty,
        port = resolveServerPort(),
        host = resolveServerHost(),
        module = Application::supportDeskModule,
    )
    engine.start(wait = wait)
    return SupportDeskServerHandle(engine)
}

fun Application.supportDeskModule() {
    configureSupportDeskModule()
}
