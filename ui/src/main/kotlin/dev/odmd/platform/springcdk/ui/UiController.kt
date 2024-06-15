package dev.odmd.platform.springcdk.ui

import dev.odmd.platform.springcdk.services.PaymentProfileService
import dev.odmd.platform.springcdk.services.PaymentService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class UiController(
    private val paymentService: PaymentService,
    private val profileService: PaymentProfileService,
    private val scopedTransitContextService: dev.odmd.platform.springcdk.domain.cryptography.ScopedTransitContextService
) {
    @GetMapping("/")
    fun searchPage(): String {
        return "search"
    }

    @GetMapping("/target")
    fun viewTarget(@RequestParam params: Map<String, String>, model: Model): String {
        val type = params["target_type"]!!
        val key = params["target_key"]!!
        val customerId = params["customer_id"]!!

        scopedTransitContextService.scopedContext = customerId.encodeToByteArray()

        val payments = paymentService.getAllPaymentsForTarget(type, key)
        model["payments"] = payments
        model["target"] = "$type:$key"
        model["customerId"] = customerId

        return "view_target"
    }

    @GetMapping("/profile")
    fun viewProfile(@RequestParam params: Map<String, String>, model: Model): String {
        val profileId = params["profile_id"]!!
        val customerId = params["customer_id"]!!

        scopedTransitContextService.scopedContext = customerId.encodeToByteArray()

        val profile = profileService.getByExternalId(customerId, profileId)!!

        model["profile"] = profile
        model["customerId"] = customerId
        model["savedCreditCard"] = profile.paymentMethod.savedCreditCard!!

        return "view_profile"
    }

    @GetMapping("/entity")
    fun viewEntity(@RequestParam params: Map<String, String>, model: Model): String {
        val customerId = params["customer_id"]!!

        scopedTransitContextService.scopedContext = customerId.encodeToByteArray()

        val profiles = profileService.getByCustomerId(customerId)

        model["profiles"] = profiles
        model["customerId"] = customerId

        return "view_entity"
    }

}
