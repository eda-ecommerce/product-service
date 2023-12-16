package org.eda.ecommerce.data.models.events

abstract class GenericEvent(type: String) {
    var source: String? = null
    var timestamp: Long? = null
    var type: String? = type

    init {
        this.source = "product-service"
        this.timestamp = System.currentTimeMillis()
    }
}
