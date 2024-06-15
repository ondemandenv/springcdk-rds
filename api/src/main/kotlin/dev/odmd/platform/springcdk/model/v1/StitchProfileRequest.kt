package dev.odmd.platform.springcdk.model.v1

import javax.validation.constraints.NotBlank

data class StitchProfileRequest(@field:NotBlank val guestSessionCustomerId: String)