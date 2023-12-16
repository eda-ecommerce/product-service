package org.eda.ecommerce.services

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eda.ecommerce.data.models.Product
import org.eda.ecommerce.data.models.events.ProductCreatedEvent
import org.eda.ecommerce.data.models.events.ProductDeletedEvent
import org.eda.ecommerce.data.models.events.ProductEvent
import org.eda.ecommerce.data.models.events.ProductUpdatedEvent
import org.eda.ecommerce.data.repositories.ProductRepository


@ApplicationScoped
class ProductService {

    @Inject
    private lateinit var productRepository: ProductRepository

    @Inject
    @Channel("product-out")
    private lateinit var productEmitter: Emitter<ProductEvent>

    fun getAll(): List<Product> {
        return productRepository.listAll()
    }

    fun findById(id: Long): Product {
        return productRepository.findById(id)
    }

    fun deleteById(id: Long): Boolean {
        val productToDelete = productRepository.findById(id) ?: return false

        productRepository.delete(productToDelete)

        val productEvent = ProductDeletedEvent(
            payload = Product().apply { this.id = id }
        )

        productEmitter.send(productEvent).toCompletableFuture().get()

        return true
    }

    fun createNewProduct(product: Product) {
        productRepository.persist(product)

        val productEvent = ProductCreatedEvent(
            payload = product
        )

        productEmitter.send(productEvent).toCompletableFuture().get()
    }

    fun updateProduct(product: Product) : Boolean {
        val entity = productRepository.findById(product.id) ?: return false

        entity.apply {
            this.color = product.color
            this.description = product.description
        }

        productRepository.persist(entity)

        val productEvent = ProductUpdatedEvent(
            payload = entity
        )

        productEmitter.send(productEvent).toCompletableFuture().get()

        return true
    }

}
