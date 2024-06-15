package dev.odmd.platform.springcdk.gateways.worldpay

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import java.util.*

@ConfigurationProperties(prefix = "app.gateway.worldpay")
@Component
class WorldPayConfiguration {
    lateinit var url: String

    lateinit var username: String

    lateinit var password: String

    lateinit var merchantId: String

    lateinit var reportGroup: String

    lateinit var registerTokenSuccessCodes: List<String>

    lateinit var transactionSuccessCodes: List<String>

    val worldpayClientProperties
        get() = Properties().also { props ->
            props["merchantId"] = merchantId
            props["url"] = url
            props["username"] = username
            props["password"] = password
            props["reportGroup"] = reportGroup
        }
}
