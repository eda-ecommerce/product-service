package org.eda.ecommerce.data.models.events

import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata
import org.apache.kafka.common.header.internals.RecordHeaders
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata

import org.eda.ecommerce.data.models.Product


open class ProductEvent(operation: String, product: Product) : Message<Product> {
    private val message: Message<Product> = createMessageWithMetadata(product, operation)

    override fun getPayload(): Product = message.payload
    override fun getMetadata(): Metadata = message.metadata
    companion object {
        private fun createMessageWithMetadata(product: Product, operation: String): Message<Product> {
            val metadata = Metadata.of(
                OutgoingKafkaRecordMetadata.builder<String>()
                    .withHeaders(RecordHeaders().apply {
                        add("operation", operation.toByteArray())
                        add("source", "product".toByteArray())
                        add("timestamp", System.currentTimeMillis().toString().toByteArray())
                    }).build()
            )
            return Message.of(product, metadata)
        }
    }
}

class ProductCreatedEvent(product: Product) : ProductEvent("created", product)

class ProductUpdatedEvent(product: Product) : ProductEvent("updated", product)

class ProductDeletedEvent(product: Product) : ProductEvent("deleted", product)