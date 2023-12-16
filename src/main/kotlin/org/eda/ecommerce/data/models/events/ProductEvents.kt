package org.eda.ecommerce.data.models.events

import org.eda.ecommerce.data.models.Product

open class ProductEvent(type: String, var payload: Product) : GenericEvent(type)

class ProductCreatedEvent(payload: Product) : ProductEvent("created", payload)

class ProductUpdatedEvent(payload: Product) : ProductEvent("updated", payload)

class ProductDeletedEvent(payload: Product) : ProductEvent("deleted", payload)
