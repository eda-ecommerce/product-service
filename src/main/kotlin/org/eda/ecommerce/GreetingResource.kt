package org.eda.ecommerce

import jakarta.enterprise.context.ApplicationScoped
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import java.util.concurrent.CompletionStage


@Path("/hello")
@ApplicationScoped
class GreetingResource {


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Returns a Hello World String")
    fun hello() = "Hello from RESTEasy Reactive"

    @Incoming("test")
    fun consume(msg: Message<Double>): CompletionStage<Void> {
        println(msg.payload)
        return msg.ack()
    }

}
