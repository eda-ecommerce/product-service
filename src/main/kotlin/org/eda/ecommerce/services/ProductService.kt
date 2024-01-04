package org.eda.ecommerce.services

import io.smallrye.reactive.messaging.MutinyEmitter
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eda.ecommerce.data.models.Product
import org.eda.ecommerce.data.models.UpdateProductDTO
import org.eda.ecommerce.data.models.events.ProductCreatedEvent
import org.eda.ecommerce.data.models.events.ProductDeletedEvent
import org.eda.ecommerce.data.models.events.ProductUpdatedEvent
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

    fun deleteById(id: UUID): Boolean {
        val productToDelete = productRepository.findById(id) ?: return false

        productRepository.delete(productToDelete)

        productEmitter.sendMessageAndAwait(ProductDeletedEvent(productToDelete))

        return true
    }

    fun createNewProduct(product: Product) {
        productRepository.persist(product)

        productEmitter.sendMessageAndAwait(ProductCreatedEvent(product))

    }

    fun updateProduct(productDTO: UpdateProductDTO): Boolean {
        val entity = productRepository.findById(productDTO.id) ?: return false

        entity.apply {
            this.status = productDTO.status ?: entity.status
            this.color = productDTO.color ?: entity.color
            this.description = productDTO.description ?: entity.description
        }

        productRepository.persist(entity)


        productEmitter.sendMessageAndAwait(ProductUpdatedEvent(entity))

        return true
    }

}
