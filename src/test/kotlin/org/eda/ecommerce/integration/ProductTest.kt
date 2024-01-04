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
import org.eda.ecommerce.data.models.Product
import org.eda.ecommerce.data.models.ProductStatus
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

    lateinit var consumer: KafkaConsumer<String, Product>

    @Inject
    lateinit var productRepository: ProductRepository

    @BeforeAll
    fun setup() {
        val productJsonSerdeFactory = JsonSerdeFactory<Product>()

        consumer = KafkaConsumer(
            consumerConfig(),
            StringDeserializer(),
            productJsonSerdeFactory.createDeserializer(Product::class.java)
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

        val records: ConsumerRecords<String, Product> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList().first()
        val eventPayload = event.value()
        val eventHeaders = event.headers().toList().associateBy({ it.key() }, { it.value().toString(Charsets.UTF_8) })

        Assertions.assertEquals("product", eventHeaders["source"])
        Assertions.assertEquals("created", eventHeaders["operation"])
        Assertions.assertEquals(ProductStatus.ACTIVE, eventPayload.status)
        Assertions.assertEquals(jsonBody.getValue("color"), eventPayload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), eventPayload.description)
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

        val records: ConsumerRecords<String, Product> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList()[1]
        val eventPayload = event.value()
        val eventHeaders = event.headers().toList().associateBy({ it.key() }, { it.value().toString(Charsets.UTF_8) })

        Assertions.assertEquals("product", eventHeaders["source"])
        Assertions.assertEquals("deleted", eventHeaders["operation"])
        Assertions.assertEquals(createdId, eventPayload.id)
        Assertions.assertEquals(ProductStatus.ACTIVE, eventPayload.status)
        Assertions.assertEquals(jsonBody.getValue("color"), eventPayload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), eventPayload.description)

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


        val records: ConsumerRecords<String, Product> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList()[1]
        val eventPayload = event.value()
        val eventHeaders = event.headers().toList().associateBy({ it.key() }, { it.value().toString(Charsets.UTF_8) })

        Assertions.assertEquals("product", eventHeaders["source"])
        Assertions.assertEquals("updated", eventHeaders["operation"])
        Assertions.assertEquals(createdId, eventPayload.id)
        Assertions.assertEquals(ProductStatus.ACTIVE, eventPayload.status)
        Assertions.assertEquals(jsonBodyUpdated.getValue("color"), eventPayload.color)
        Assertions.assertEquals(jsonBodyUpdated.getValue("description"), eventPayload.description)

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


        val records: ConsumerRecords<String, Product> = consumer.poll(Duration.ofMillis(10000))

        val event = records.records("product").iterator().asSequence().toList()[1]
        val eventPayload = event.value()
        val eventHeaders = event.headers().toList().associateBy({ it.key() }, { it.value().toString(Charsets.UTF_8) })

        Assertions.assertEquals("product", eventHeaders["source"])
        Assertions.assertEquals("updated", eventHeaders["operation"])
        Assertions.assertEquals(createdId, eventPayload.id)
        Assertions.assertEquals(ProductStatus.RETIRED, eventPayload.status)
        Assertions.assertEquals(jsonBody.getValue("color"), eventPayload.color)
        Assertions.assertEquals(jsonBody.getValue("description"), eventPayload.description)

        Assertions.assertEquals(1, productRepository.count())
    }

}
