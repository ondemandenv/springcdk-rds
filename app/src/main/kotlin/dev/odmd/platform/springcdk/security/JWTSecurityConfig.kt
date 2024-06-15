package dev.odmd.platform.springcdk.security

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jwt.JwtClaimNames.AUD
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoders.fromOidcIssuerLocation
import org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfiguration.ALL
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.util.stream.Collectors.toMap

//@Configuration
//@EnableWebSecurity
//@EnableConfigurationProperties(SecurityProperties::class)
//@ConditionalOnExpression("\${app.security.enabled:false}")
/**
 * Disabled until we identify clients who will send us JWTs
 */
class JWTSecurityConfig(private val securityProperties: SecurityProperties) : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http {
            cors { }
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = STATELESS
            }
            authorizeRequests {
                authorize(toAnyEndpoint(), permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-resources/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)

                authorize(anyRequest, authenticated)
            }
            oauth2ResourceServer {
                val authenticationManagers = securityProperties.tenants.stream()
                    .collect(toMap(TenantProperties::issuerUri, ::authenticationManager))
                authenticationManagerResolver = JwtIssuerAuthenticationManagerResolver(authenticationManagers::get)
            }
        }
    }

    @Bean
    @Profile("local")
    fun corsConfigurationSource() = UrlBasedCorsConfigurationSource().apply {
        val corsConfiguration = CorsConfiguration().apply {
            allowedOrigins = listOf(ALL)
            allowedMethods = listOf(ALL)
            allowedHeaders = listOf(ALL)
            exposedHeaders = listOf(ALL)
        }
        registerCorsConfiguration("/**", corsConfiguration)
    }

    private fun authenticationManager(tenant: TenantProperties) =
        authenticationManager(JwtAuthenticationProvider(jwtDecoder(tenant)))

    private fun authenticationManager(authenticationProvider: JwtAuthenticationProvider) =
        AuthenticationManager(authenticationProvider::authenticate)

    private fun jwtDecoder(tenant: TenantProperties) =
        fromOidcIssuerLocation<NimbusJwtDecoder>(tenant.issuerUri).apply {
            setJwtValidator(oAuth2TokenValidator(tenant))
        }

    private fun oAuth2TokenValidator(tenant: TenantProperties) =
        DelegatingOAuth2TokenValidator(createDefaultWithIssuer(tenant.issuerUri), audienceValidator(tenant.audience))

    private fun audienceValidator(audience: String) =
        JwtClaimValidator<List<String>?>(AUD) { it?.contains(audience) ?: false }
}
