package dev.odmd.platform.springcdk.domain.cryptography

import org.springframework.beans.factory.annotation.Autowired


class TransitConfiguration {
    companion object {
        lateinit var cryptographyService: dev.odmd.platform.springcdk.domain.cryptography.CryptographyService
        lateinit var transitContextService: dev.odmd.platform.springcdk.domain.cryptography.TransitContextService
    }

    /**
     * **HACK**: Spring autowires the beans into this class, and we set them into the companion object.
     *
     * [ContextualTransitDeserializer] and [TransitSerializer] are constructed in a Hibernate thread, so we can't pull beans from the Spring
     * application context, but we still need access to them in order to deserialize from the Vault transit engine.
     *
     * This will only be called once at app startup, which is fine "in production", but can be
     * problematic during testing since application contexts all share the same global state.
     * See [ResetsTransitConfiguration] for a workaround.
     */
    @Autowired
    fun setBeansFromSpringContext(
        cryptographyService: dev.odmd.platform.springcdk.domain.cryptography.CryptographyService,
        transitContextService: dev.odmd.platform.springcdk.domain.cryptography.TransitContextService
    ) {
        Companion.cryptographyService = cryptographyService
        Companion.transitContextService = transitContextService
    }
}
