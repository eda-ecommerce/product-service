package org.eda.ecommerce

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.MediaType
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import java.util.concurrent.CompletableFuture

@Path("emit")
@ApplicationScoped
class KafkaEmitterOnPost {

    @Inject
    @Channel("test-out")
    private lateinit var testEmitter: Emitter<String>


    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Accepts a plain Body that is interpreted as a String and send as an Event into the 'test' topic.")
    fun addPrice(price: String?) {
        testEmitter.send(
            Message.of(price)
                .withAck {
                    println("Acked")
                    CompletableFuture.completedFuture(
                        null
                    )
                }
                .withNack { throwable: Throwable? ->
                    println("Nacked")
                    CompletableFuture.completedFuture(
                        null
                    )
                })
    }

}
