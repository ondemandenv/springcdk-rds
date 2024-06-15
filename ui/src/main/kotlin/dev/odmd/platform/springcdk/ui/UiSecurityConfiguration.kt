package dev.odmd.platform.springcdk.ui

import com.azure.spring.cloud.autoconfigure.aad.AadWebSecurityConfigurerAdapter
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.servlet.HttpSecurityDsl
import org.springframework.security.config.web.servlet.invoke

/**
 * Main security configuration used when Azure Active Directory is enabled.
 */
@Configuration
@ConditionalOnProperty("spring.cloud.azure.active-directory.enabled")
class UiSecurityConfiguration : AadWebSecurityConfigurerAdapter() {
    override fun configure(http: HttpSecurity) {
        super.configure(http)
        http {
            applyUiSecurityConfig()
        }
    }
}

/**
 * Extension function to apply the security configuration which requires Admin role
 * on all endpoints *except* actuator.
 *
 * This is done to ensure the security configuration is the same in production
 * and mock MVC tests.
 */
fun HttpSecurityDsl.applyUiSecurityConfig() =
    authorizeRequests {
        // toAnyEndpoint is documented as matching all *actuator* endpoints
        authorize(EndpointRequest.toAnyEndpoint(), permitAll)
        authorize("/**", hasAuthority("APPROLE_Admin"))
    }
