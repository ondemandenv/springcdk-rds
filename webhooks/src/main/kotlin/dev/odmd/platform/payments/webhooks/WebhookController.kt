package dev.odmd.platform.springcdk.webhooks

import com.stripe.model.Invoice
import com.stripe.net.Webhook
import io.swagger.v3.oas.annotations.Hidden
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

const val WEBHOOK_ROOT_PATH = "/webhooks"
private const val STRIPE_SIGNING_TOLERANCE = 300L

@RestController
@RequestMapping(WEBHOOK_ROOT_PATH)
@Hidden
internal class WebhookController(
    private val webhookConfiguration: StripeWebhookConfiguration,
    private val invoiceWebhookProcessor: InvoiceWebhookProcessor,
) {

    @PostMapping("/stripe/invoice")
    fun handleInvoiceWebhookEvent(
        @RequestHeader("Stripe-Signature") stripeAuthHeader: String,
        @RequestBody stripeWebhookEvent: String
    ): ResponseEntity<out Unit> {
        val webhookEvent = Webhook.constructEvent(
            stripeWebhookEvent, stripeAuthHeader, webhookConfiguration.signingSecret, STRIPE_SIGNING_TOLERANCE
        )
        val invoice = webhookEvent.dataObjectDeserializer.deserializeUnsafe() as Invoice
        val processInvoiceRequest =
            ProcessInvoiceRequest(invoice, webhookEvent)

        return ResponseEntity.ok(invoiceWebhookProcessor.processInvoiceEvent(processInvoiceRequest).getOrThrow())
    }
}

@ConfigurationProperties(prefix = "app.gateway.stripe.webhooks")
internal class StripeWebhookConfiguration {
    lateinit var signingSecret: String
}
