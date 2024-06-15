package dev.odmd.platform.springcdk.controllers

import dev.odmd.platform.springcdk.api.v1.WorldPayHttpService
import dev.odmd.platform.springcdk.gateways.worldpay.proxy.WorldPayProxyService
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import javax.servlet.http.HttpServletRequest


@RestController
class WorldPayController(val worldPayProxyService: WorldPayProxyService) :
    dev.odmd.platform.springcdk.api.v1.WorldPayHttpService {

    override fun proxy(request: ByteArray, httpServletRequest: HttpServletRequest): ResponseEntity<ByteArray> {
        val httpMethod = HttpMethod.valueOf(httpServletRequest.method)
        return worldPayProxyService.proxyRequest(
            httpMethod,
            InputStreamReader(ByteArrayInputStream(request))
        )
    }
}
