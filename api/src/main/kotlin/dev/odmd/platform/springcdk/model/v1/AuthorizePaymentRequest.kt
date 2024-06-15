package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.model.v1.validation.ValidBillingDescriptor
import javax.validation.Valid
import javax.validation.constraints.NotBlank

data class AuthorizePaymentRequest(
    @field: NotBlank
    val requestId: dev.odmd.platform.springcdk.model.v1.RequestId,

    val currencyCode: String,

    @field: NotBlank
    val targetType: String,

    @field: NotBlank
    val targetKey: String,

    @field: NotBlank
    val reason: String,

    @field: NotBlank
    val source: String,

    @field:ValidBillingDescriptor
    val billingDescriptor: String? = null,

    @field:Valid
    val lineItemDtos: List<dev.odmd.platform.springcdk.model.v1.LineItemDto>,

    @field:Valid
    val paymentDtos: List<dev.odmd.platform.springcdk.model.v1.PaymentDto>
)
