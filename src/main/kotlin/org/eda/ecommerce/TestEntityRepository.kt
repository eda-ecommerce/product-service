package org.eda.ecommerce

import io.quarkus.hibernate.orm.panache.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class TestEntityRepository : PanacheRepository<TestEntity> {

    @Transactional
    fun persistWithTransaction (testEntity: TestEntity) {
        super.persist(testEntity)
    }
}
