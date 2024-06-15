package dev.odmd.platform.springcdk.webhooks

import com.stripe.model.Event
import com.stripe.model.Invoice

internal data class ProcessInvoiceRequest(
    val invoice: Invoice,
    val event: Event,
)

internal enum class EventType(val value: String) {
    ACTION_REQUIRED("invoice.payment_action_required"),
    SUCCESS("invoice.payment_succeeded"),
    FAILED("invoice.payment_failed");

    companion object {
        fun fromEventType(eventType: String) =
            when (eventType) {
                "invoice.payment_action_required" -> ACTION_REQUIRED
                "invoice.payment_succeeded" -> SUCCESS
                "invoice.payment_failed" -> FAILED
                else -> throw InvalidEventReceived(eventType)
            }
    }

}
