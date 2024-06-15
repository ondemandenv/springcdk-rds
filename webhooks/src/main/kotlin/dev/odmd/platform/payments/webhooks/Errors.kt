package dev.odmd.platform.springcdk.webhooks

internal class NoProfileForCustomerId(customerId: String) :
    Exception("No Profile found for customer [$customerId]")

internal class InvalidEventReceived(eventType: String) :
    Exception("Not configured to handle [$eventType]")

internal class AlreadyRecordedSuccess(paymentId: Long) :
    Exception("Received successful PaymentTransaction when success already exists for payment[$paymentId]")
