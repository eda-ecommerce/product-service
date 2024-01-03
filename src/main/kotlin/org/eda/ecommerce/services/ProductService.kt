package org.eda.ecommerce.services

import io.smallrye.reactive.messaging.MutinyEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.eda.ecommerce.data.models.Product
import org.eda.ecommerce.data.models.UpdateProductDTO
import org.eda.ecommerce.data.repositories.ProductRepository
import java.util.*


@ApplicationScoped
class ProductService {

    @Inject
    private lateinit var productRepository: ProductRepository

    @Inject
    @Channel("product-out")
    private lateinit var productEmitter: MutinyEmitter<Product>

    fun getAll(): List<Product> {
        return productRepository.listAll()
    }

    fun findById(id: UUID): Product {
        return productRepository.findById(id)
    }

    fun createMessageWithMetadata(product: Product, type: String): Message<Product> {
        val metadataMap = mapOf(
            "type" to type,
            "source" to "product",
            "timestamp" to System.currentTimeMillis()
        )

        return Message.of(product, Metadata.of(metadataMap))
    }

    fun deleteById(id: UUID): Boolean {
        val productToDelete = productRepository.findById(id) ?: return false

        productRepository.delete(productToDelete)

        productEmitter.sendMessageAndAwait(createMessageWithMetadata(productToDelete, "deleted"))

        return true
    }

    fun createNewProduct(product: Product) {
        productRepository.persist(product)

        productEmitter.sendMessageAndAwait(createMessageWithMetadata(product, "created"))

    }

    fun updateProduct(productDTO: UpdateProductDTO): Boolean {
        val entity = productRepository.findById(productDTO.id) ?: return false

        entity.apply {
            this.status = productDTO.status ?: entity.status
            this.color = productDTO.color ?: entity.color
            this.description = productDTO.description ?: entity.description
        }

        productRepository.persist(entity)


        productEmitter.sendMessageAndAwait(createMessageWithMetadata(entity, "updated"))

        return true
    }

}
