package dev.odmd.platform.springcdk.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
data class ProfileTokenNotFound(val profileId: String) :
    RuntimeException("Unable to retrieve token for profile with id $profileId")
