package org.eda.ecommerce.integration

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.kafka.InjectKafkaCompanion
import io.quarkus.test.kafka.KafkaCompanionResource
import io.restassured.RestAssured.given
import io.smallrye.reactive.messaging.kafka.companion.ConsumerTask
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eda.ecommerce.JsonSerdeFactory
import org.eda.ecommerce.data.models.events.ProductEvent
import org.eda.ecommerce.data.repositories.ProductRepository
import org.junit.jupiter.api.*


@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductTest {
    @InjectKafkaCompanion
    lateinit var companion: KafkaCompanion

    @Inject
    lateinit var productRepository: ProductRepository

    @BeforeAll
    fun setup() {
        val productJsonSerdeFactory = JsonSerdeFactory<ProductEvent>()
        companion.registerSerde(
            ProductEvent::class.java,
            productJsonSerdeFactory.createSerializer(),
            productJsonSerdeFactory.createDeserializer(ProductEvent::class.java)
        )
    }

    @BeforeEach
    @Transactional
    fun recreateTestedTopics() {
        companion.topics().delete("product")
        companion.topics().create("product", 1)
        productRepository.deleteAll()
    }

    @Test
    fun testCreationAndPersistenceOnPost() {
        val jsonBody: JsonObject = JsonObject()
            .put("color", "red")
            .put("description", "A red thing")

        given()
            .contentType("application/json")
            .body(jsonBody.toString())
            .`when`().post("/products")
            .then()
            .statusCode(201)

        Assertions.assertEquals(1, productRepository.count())
        Assertions.assertEquals(jsonBody.getValue("color"), productRepository.findById(1L).color)
        Assertions.assertEquals(jsonBody.getValue("description"), productRepository.findById(1L).description)
    }

    @Test
    fun testKafkaEmitOnPost() {
        val jsonBody: JsonObject = JsonObject()
            .put("color", "green")
            .put("description", "A green thing")

        given()
            .contentType("application/json")
            .body(jsonBody.toString())
            .`when`().post("/products")
            .then()
            .statusCode(201)

        val productConsumer: ConsumerTask<String, ProductEvent> =
            companion.consume(ProductEvent::class.java).fromTopics("product", 1)

        productConsumer.awaitCompletion()

        val event = productConsumer.firstRecord.value()
        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("created", event.type)
        Assertions.assertEquals(jsonBody.getValue("color"), event.payload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), event.payload.description)
    }

    @Test
    fun testDelete() {
        val jsonBody: JsonObject = JsonObject()
            .put("color", "orange")
            .put("description", "An orange thing")

        given()
            .contentType("application/json")
            .body(jsonBody.toString())
            .`when`().post("/products")
            .then()
            .statusCode(201)

        Assertions.assertEquals(1, productRepository.count())

        val createdId = productRepository.listAll()[0].id

        given()
            .contentType("application/json")
            .`when`()
            .queryParam("id", createdId)
            .delete("/products")
            .then()
            .statusCode(202)

        val productConsumer: ConsumerTask<String, ProductEvent> =
            companion.consume(ProductEvent::class.java).fromTopics("product", 2)

        productConsumer.awaitCompletion()

        val event = productConsumer.records[1].value()
        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("deleted", event.type)
        Assertions.assertEquals(createdId, event.payload.id)
        Assertions.assertEquals(null, event.payload.color)
        Assertions.assertEquals(null, event.payload.description)

        Assertions.assertEquals(0, productRepository.count())
    }

}
