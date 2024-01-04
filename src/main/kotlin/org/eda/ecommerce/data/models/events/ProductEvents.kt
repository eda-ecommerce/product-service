package org.eda.ecommerce.data.models.events

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata

import org.eda.ecommerce.data.models.Product


open class ProductEvent(type: String, payload: Product) : Message<Product> {
    private val message: Message<Product> = createMessageWithMetadata(payload, type)

    override fun getPayload(): Product = message.payload
    override fun getMetadata(): Metadata = message.metadata
    companion object {
        private fun createMessageWithMetadata(product: Product, type: String): Message<Product> {
            val metadata = Metadata.of(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(RecordHeaders().apply {
                        add("operation", type.toByteArray())
                        add("source", "product".toByteArray())
                        add("timestamp", System.currentTimeMillis().toString().toByteArray())
                    }).build()
            )
            return Message.of(product, metadata)
        }
    }
}

class ProductCreatedEvent(payload: Product) : ProductEvent("created", payload)

class ProductUpdatedEvent(payload: Product) : ProductEvent("updated", payload)

class ProductDeletedEvent(payload: Product) : ProductEvent("deleted", payload)