package dev.odmd.platform.springcdk.model.v1

import dev.odmd.platform.springcdk.common.CurrencyAmount
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank

data class LineItemDto(

    @field: NotBlank
    val id: String,

    val description: String,

    @field:DecimalMin("0.0")
    val amount: CurrencyAmount
)
