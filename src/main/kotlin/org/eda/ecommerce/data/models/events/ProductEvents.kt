package org.eda.ecommerce.data.models.events

import org.eda.ecommerce.data.models.Product

open class ProductEvent(type: String, var content: Product) : GenericEvent(type)

class ProductCreatedEvent(content: Product) : ProductEvent("created", content)

class ProductUpdatedEvent(content: Product) : ProductEvent("updated", content)

class ProductDeletedEvent(content: Product) : ProductEvent("deleted", content)
