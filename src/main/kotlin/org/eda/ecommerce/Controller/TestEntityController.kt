package org.eda.ecommerce.Controller

import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eda.ecommerce.TestEntity
import org.eda.ecommerce.TestEntityRepository
import java.net.URI

@Path("/entity")
class TestEntityController {

    @Inject
    lateinit var testEntityRepository: TestEntityRepository

    @Inject
    @Channel("test-entity-out")
    private lateinit var testEntityEmitter: Emitter<TestEntity>


    @GET
    fun getAll(): List<TestEntity> {
        return testEntityRepository.listAll()
    }

    @GET
    @Path("/{id}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Operation(summary = "Returns a TestEntity by its ID.")
    fun getById(
        @QueryParam("id")
        @Parameter(
            name = "id",
            description = "The ID of the TestEntity to be returned.",
            schema = Schema(type = SchemaType.NUMBER, format = "long")
        )
        id: Long
    ): TestEntity {
        return testEntityRepository.findById(id)
    }

    @POST
    fun createNew(testEntity: TestEntity): Response {
        testEntityRepository.persistWithTransaction(testEntity)

        testEntityEmitter.send(testEntity)

        return Response.created(URI.create("/entity/" + testEntity.id)).build()
    }

    @Incoming("test-entity-in")
    fun consume(value: TestEntity) {
        println(value)
    }
}
