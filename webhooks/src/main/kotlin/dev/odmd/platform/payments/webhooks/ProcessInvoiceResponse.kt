package dev.odmd.platform.springcdk.webhooks

sealed class ProcessInvoiceResponse
object ProcessInvoiceSuccess: ProcessInvoiceResponse()
object ProcessInvoiceFailure: ProcessInvoiceResponse()
