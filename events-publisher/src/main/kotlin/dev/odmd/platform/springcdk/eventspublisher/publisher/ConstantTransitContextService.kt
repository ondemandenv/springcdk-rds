package dev.odmd.platform.springcdk.eventspublisher.publisher

import dev.odmd.platform.springcdk.domain.cryptography.TransitContextService
import org.springframework.stereotype.Service

@Service
class ConstantTransitContextService() : dev.odmd.platform.springcdk.domain.cryptography.TransitContextService {
    private var currentContext = byteArrayOf()

    fun setContext(context: ByteArray) {
        currentContext = context
    }
    override fun getContext(): ByteArray {
        return currentContext
    }
}