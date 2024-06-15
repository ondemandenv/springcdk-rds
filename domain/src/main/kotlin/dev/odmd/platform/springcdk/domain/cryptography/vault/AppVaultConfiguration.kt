package dev.odmd.platform.springcdk.domain.cryptography.vault

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties(prefix = "app.vault")
@Component
class AppVaultConfiguration {
    lateinit var keyName: String

    /**
     * Transit engine name without trailing `/`
     */
    lateinit var transitPath: String

    /**
     * This controls which [ObjectMapper][com.fasterxml.jackson.databind.ObjectMapper] is used - when enabled is "true",
     * we use [EncryptionObjectMapperSupplier] which uses Vault's Transit engine. When not true, we use
     * [NoEncryptionObjectMapperSupplier]. The non-"true" setting should only be used for local development
     *
     * We have `unused` suppressed because this property is used in
     * [ConditionalOnProperty][org.springframework.boot.autoconfigure.condition.ConditionalOnProperty]
     * annotations, which the IDE cannot find.
     */
    @Suppress("unused")
    lateinit var enabled: String
}
