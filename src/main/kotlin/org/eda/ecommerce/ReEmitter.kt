package org.eda.ecommerce

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming


@ApplicationScoped
class ReEmitter {

    @Inject
    @Channel("test-acknowledged-out")
    private lateinit var testAcknowledgedEmitter: Emitter<String>

    @Incoming("test-in")
    // Re-emits any events received via the `test-in` channel (kafka topic 'test') to the `test-acknowledged-out` channel (kafka topic 'test-acknowledged').
    fun consume(value: String) {
        println(value)

        testAcknowledgedEmitter.send(value)
    }
}
