package dev.odmd.platform.springcdk.security


import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest.toAnyEndpoint
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy.STATELESS
import org.springframework.security.config.web.servlet.invoke
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfiguration.ALL
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import javax.servlet.Filter

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class EntityIdHeaderWebSecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        http {
            httpBasic { disable() }
            cors { }
            csrf { disable() }
            logout { disable() }
            sessionManagement {
                sessionCreationPolicy = STATELESS
            }
            addFilterBefore<AnonymousAuthenticationFilter>(getFilter())
            authorizeRequests {
                authorize("/v3/**", permitAll)
                authorize("/webhooks/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/worldpay/**", permitAll)
                // toAnyEndpoint() actually means to any actuator endpoint
                authorize(toAnyEndpoint(), permitAll)
                authorize("/**", authenticated)
            }
        }
    }

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(EntityIdTokenAuthenticationProvider())
    }

    fun getFilter(): Filter {
        return EntityIdAuthenticationProcessingFilter(
            OrRequestMatcher(
                AntPathRequestMatcher("/payment-profiles/**"),
                AntPathRequestMatcher("/payments/**"),
                AntPathRequestMatcher("/installment-agreement/**")
            ), authenticationManager()
        )
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
}
