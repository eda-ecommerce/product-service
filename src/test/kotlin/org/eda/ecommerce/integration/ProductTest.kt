package org.eda.ecommerce.integration

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.kafka.InjectKafkaCompanion
import io.quarkus.test.kafka.KafkaCompanionResource
import io.restassured.RestAssured.given
import io.smallrye.common.annotation.Identifier
import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion
import io.vertx.core.json.JsonObject
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.eda.ecommerce.JsonSerdeFactory
import org.eda.ecommerce.data.models.ProductStatus
import org.eda.ecommerce.data.models.events.ProductEvent
import org.eda.ecommerce.data.repositories.ProductRepository
import org.eda.ecommerce.helpers.KafkaTestHelper
import org.junit.jupiter.api.*
import java.time.Duration
import java.util.*


@QuarkusTest
@QuarkusTestResource(KafkaCompanionResource::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProductTest {
    @InjectKafkaCompanion
    lateinit var companion: KafkaCompanion

    @Inject
    @Identifier("default-kafka-broker")
    lateinit var kafkaConfig: Map<String, Any>

    lateinit var consumer: KafkaConsumer<String, ProductEvent>

    @Inject
    lateinit var productRepository: ProductRepository

    @BeforeAll
    fun setup() {
        val productJsonSerdeFactory = JsonSerdeFactory<ProductEvent>()

        consumer = KafkaConsumer(
            consumerConfig(),
            StringDeserializer(),
            productJsonSerdeFactory.createDeserializer(ProductEvent::class.java)
        )
    }

    fun consumerConfig(): Properties {
        val properties = Properties()
        properties.putAll(kafkaConfig)
        properties[ConsumerConfig.GROUP_ID_CONFIG] = "test-group-id"
        properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        return properties
    }

    @BeforeEach
    @Transactional
    fun recreateTestedTopics() {
        KafkaTestHelper.clearTopicIfNotEmpty(companion, "product")
        productRepository.deleteAll()
        consumer.subscribe(listOf("product"))
    }

    @AfterEach
    fun unsubscribeConsumer() {
        consumer.unsubscribe()
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

        val createdId = productRepository.listAll()[0].id

        Assertions.assertEquals(jsonBody.getValue("color"), productRepository.findById(createdId).color)
        Assertions.assertEquals(ProductStatus.ACTIVE, productRepository.findById(createdId).status)
        Assertions.assertEquals(jsonBody.getValue("description"), productRepository.findById(createdId).description)
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

        val records: ConsumerRecords<String, ProductEvent> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList().map { it.value() }.first()

        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("created", event.type)
        Assertions.assertEquals(ProductStatus.ACTIVE, event.payload.status)
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

        val records: ConsumerRecords<String, ProductEvent> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList().map { it.value() }[1]

        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("deleted", event.type)
        Assertions.assertEquals(createdId, event.payload.id)
        Assertions.assertEquals(ProductStatus.ACTIVE, event.payload.status)
        Assertions.assertEquals(jsonBody.getValue("color"), event.payload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), event.payload.description)

        Assertions.assertEquals(0, productRepository.count())
    }

    @Test
    fun testAttributeUpdate() {
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

        val jsonBodyUpdated: JsonObject = JsonObject()
            .put("id", createdId)
            .put("color", "green")
            .put("description", "An (orange) green thing")

        given()
            .contentType("application/json")
            .body(jsonBodyUpdated.toString())
            .`when`()
            .put("/products")
            .then()
            .statusCode(202)


        val records: ConsumerRecords<String, ProductEvent> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList().map { it.value() }[1]

        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("updated", event.type)
        Assertions.assertEquals(createdId, event.payload.id)
        Assertions.assertEquals(ProductStatus.ACTIVE, event.payload.status)
        Assertions.assertEquals(jsonBodyUpdated.getValue("color"), event.payload.color)
        Assertions.assertEquals(jsonBodyUpdated.getValue("description"), event.payload.description)

        Assertions.assertEquals(1, productRepository.count())
    }

    @Test
    fun testStatusUpdate() {
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

        val jsonBodyUpdated: JsonObject = JsonObject()
            .put("id", createdId)
            .put("status", "retired")

        given()
            .contentType("application/json")
            .body(jsonBodyUpdated.toString())
            .`when`()
            .put("/products")
            .then()
            .statusCode(202)


        val records: ConsumerRecords<String, ProductEvent> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList().map { it.value() }[1]

        Assertions.assertEquals("product-service", event.source)
        Assertions.assertEquals("updated", event.type)
        Assertions.assertEquals(createdId, event.payload.id)
        Assertions.assertEquals(jsonBodyUpdated.getValue("status"), event.payload.status.value)
        Assertions.assertEquals(jsonBody.getValue("color"), event.payload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), event.payload.description)

        Assertions.assertEquals(1, productRepository.count())
    }

}
