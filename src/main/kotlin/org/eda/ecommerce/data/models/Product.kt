package org.eda.ecommerce.data.models

import com.fasterxml.jackson.annotation.JsonValue
import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import org.hibernate.annotations.GenericGenerator
import java.util.*

@Entity
class Product : PanacheEntityBase() {
    @Id
    @Column(name = "id")
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    var id: UUID? = null


    var color: String? = null
    var description: String? = null
    var status: ProductStatus = ProductStatus.ACTIVE

    override fun toString(): String {
        return "Product(id=${id}, status=$status, color=$color, description=$description))"
    }
}

class CreateProductDTO {
    var color: String? = null
    var description: String? = null

    fun toProduct(): Product {
        val product = Product()
        product.color = color
        product.description = description
        return product
    }

    override fun toString(): String {
        return "Product(color=$color, description=$description)"
    }
}

class UpdateProductDTO {
    var id: UUID? = null
    var status: ProductStatus? = ProductStatus.ACTIVE
    var color: String? = null
    var description: String? = null

    override fun toString(): String {
        return "Product(id=$id, status=$status, color=$color, description=$description)"
    }
}

enum class ProductStatus(@JsonValue val value: String) {
    ACTIVE("active"),
    RETIRED("retired");
}
