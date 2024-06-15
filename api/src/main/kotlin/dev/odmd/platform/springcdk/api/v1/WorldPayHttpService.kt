package dev.odmd.platform.springcdk.api.v1

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import javax.servlet.http.HttpServletRequest

@Hidden
@RequestMapping("worldpay")
interface WorldPayHttpService {
    @RequestMapping
    fun proxy(@RequestBody request: ByteArray, httpServletRequest: HttpServletRequest): ResponseEntity<ByteArray>
}
