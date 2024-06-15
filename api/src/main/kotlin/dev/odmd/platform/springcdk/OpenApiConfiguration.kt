package dev.odmd.platform.springcdk

import dev.odmd.platform.springcdk.common.logging.DatadogJsonLayout
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun globalHeaders(): OperationCustomizer {
        return OperationCustomizer { operation, _ ->
            val customerIdParam = Parameter()
                .`in`(ParameterIn.HEADER.toString())
                .schema(StringSchema())
                .name("odmd-eee-id")
                .description("Id of the entity that owns the payment information. Should be globally unique")
                .required(true)
            val lzCorrelationIdParam = Parameter()
                .`in`(ParameterIn.HEADER.toString())
                .schema(StringSchema())
                .name(DatadogJsonLayout.LZ_CORRELATION_ID_HEADER)
                .description("LZ correlationId header, used to correlate requests across lz systems")
                .required(false)
            operation.addParametersItem(customerIdParam)
            operation.addParametersItem(lzCorrelationIdParam)
        }
    }
}
