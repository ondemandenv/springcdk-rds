package dev.odmd.platform.springcdk.common.json

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.core.ResolvableType

inline fun <reified T> JacksonTester(objectMapper: ObjectMapper) =
    org.springframework.boot.test.json.JacksonTester<T>(
        T::class.java,
        ResolvableType.forClass(T::class.java),
        objectMapper
    )
