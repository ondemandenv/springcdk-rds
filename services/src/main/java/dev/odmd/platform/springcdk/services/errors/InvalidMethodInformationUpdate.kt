package dev.odmd.platform.springcdk.services.errors

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_REQUEST)
data class InvalidMethodInformationUpdate(
    val updateType: String,
    val profileExternalId: String
) : PaymentServiceError,
    Exception("Cannot apply $updateType to profile $profileExternalId since it has a different type of method information.")
