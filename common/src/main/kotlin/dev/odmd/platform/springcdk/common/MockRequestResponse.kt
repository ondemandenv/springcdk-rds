package dev.odmd.platform.springcdk.common

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

data class MockRequestResponse(val expectedRequest: JsonNode, val expectedResponse: JsonNode) {
    companion object {
        private val objectMapper = ObjectMapper().findAndRegisterModules()

        fun fromHeaders(expectedRequest: String?, expectedResponse: String?): MockRequestResponse? =
            if (expectedRequest != null && expectedResponse != null) {
                MockRequestResponse(expectedRequest, expectedResponse)
            } else {
                null
            }
    }

    constructor(expectedRequest: String, expectedResponse: String)
        : this(objectMapper.readTree(expectedRequest), objectMapper.readTree(expectedResponse))
}
