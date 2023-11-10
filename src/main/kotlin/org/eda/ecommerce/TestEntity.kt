package org.eda.ecommerce

import io.quarkus.hibernate.orm.panache.PanacheEntity
import jakarta.persistence.Entity

@Entity
class TestEntity : PanacheEntity() {
    var value: String? = null

    override fun toString(): String {
        return "TestEntity(id=$id, value=$value)"
    }
}

