package dev.odmd.platform.springcdk.common

import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

internal class MockRequestResponseTest {
    @Test
    fun `fromHeaders returns null if request is null`() {
        assertNull(MockRequestResponse.fromHeaders(expectedRequest = null, expectedResponse = ""))
    }

    @Test
    fun `fromHeaders returns null if response is null`() {
        assertNull(MockRequestResponse.fromHeaders(expectedRequest = "", expectedResponse = null))
    }

    @Test
    fun `fromHeaders calls string constructor when both arguments are present`() {
        val objectMapper = ObjectMapper().findAndRegisterModules()
        val requestMap = mapOf(
            "foo" to "bar"
        )
        val responseMap = mapOf(
            "fizz" to "buzz"
        )
        val (requestJson, responseJson) = listOf(requestMap, responseMap).map {
            objectMapper.writeValueAsString(it)
        }

        val mockRequestResponse = MockRequestResponse.fromHeaders(requestJson, responseJson)
            ?: fail("expected fromHeaders to return nonnull object")

        assertThat(mockRequestResponse.expectedRequest.get("foo")?.asText(), equalTo(requestMap["foo"]))
        assertThat(mockRequestResponse.expectedResponse.get("fizz")?.asText(), equalTo(responseMap["fizz"]))
    }
}
