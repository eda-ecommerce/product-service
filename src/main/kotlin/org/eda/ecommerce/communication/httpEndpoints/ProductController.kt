package org.eda.ecommerce.communication.httpEndpoints

import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses
import org.eda.ecommerce.data.models.CreateProductDTO
import org.eda.ecommerce.data.models.Product
import org.eda.ecommerce.data.models.UpdateProductDTO
import org.eda.ecommerce.services.ProductService
import java.net.URI
import java.util.*

@Path("/products")
class ProductController {

    @Inject
    private lateinit var productService: ProductService


    @GET
    @Operation(summary = "Returns a list of all Products")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "A list of Products.",
            content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = Array<Product>::class))]
        )
    )
    fun getAll(): List<Product> {
        return productService.getAll()
    }

    @GET
    @Path("{id}")
    @Operation(summary = "Returns a Product by its ID.")
    @APIResponses(
        APIResponse(
            responseCode = "200",
            description = "The Product with the given ID.",
            content = [Content(mediaType = MediaType.APPLICATION_JSON, schema = Schema(implementation = Product::class))]
        ),
        APIResponse(responseCode = "404", description = "Product not found")
    )
    @Consumes(MediaType.TEXT_PLAIN)
    fun getById(
        @PathParam("id")
        @Parameter(
            name = "id",
            description = "The ID of the Product to be returned.",
            schema = Schema(type = SchemaType.STRING, format = "UUID")
        )
        id: UUID
    ): Product {
        return productService.findById(id)
    }

    @POST
    @Operation(summary = "Create and store a new Product")
    @APIResponses(
        APIResponse(responseCode = "201", description = "Product created"),
    )
    @Transactional
    fun createNewProduct(productDTO: CreateProductDTO): Response {
        val product = productDTO.toProduct()

        productService.createNewProduct(product)

        return Response.created(URI.create("/products/" + product.id)).build()
    }

    @PUT
    @Operation(summary = "Update an existing Product")
    @APIResponses(
        APIResponse(responseCode = "202", description = "Product updated"),
        APIResponse(responseCode = "400", description = "Invalid Product data supplied"),
        APIResponse(responseCode = "404", description = "Product not found")
    )
    @Transactional
    fun updateProduct(product: UpdateProductDTO): Response {
        val updated = productService.updateProduct(product)

        return if (updated)
            Response.status(Response.Status.ACCEPTED).build()
        else
            Response.status(Response.Status.NOT_FOUND).build()
    }

    @DELETE
    @Path("{id}")
    @Operation(summary = "Delete an existing Product")
    @APIResponses(
        APIResponse(responseCode = "202", description = "Product deleted"),
        APIResponse(responseCode = "404", description = "Product not found")
    )
    @Transactional
    fun deleteProductById(
        @PathParam("id")
        @Parameter(
            name = "id",
            description = "The ID of the Product to be deleted.",
            schema = Schema(type = SchemaType.STRING, format = "UUID")
        )
        id: UUID
    ): Response {
        val deleted = productService.deleteById(id)

        return if (deleted)
            Response.status(Response.Status.ACCEPTED).build()
        else
            Response.status(Response.Status.NOT_FOUND).build()
    }
}
