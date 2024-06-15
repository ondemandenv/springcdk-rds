package dev.odmd.platform.springcdk.security

import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.InsufficientAuthenticationException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.security.web.util.matcher.RequestMatcher
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class EntityIdAuthenticationProcessingFilter(
    requiresAuthenticationRequestMatcher: RequestMatcher?,
    authenticationManager: AuthenticationManager?
) : AbstractAuthenticationProcessingFilter(requiresAuthenticationRequestMatcher, authenticationManager) {

    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
        if (request == null) {
            throw InsufficientAuthenticationException("Null HTTP servlet request")
        }

        val entityId = request.getHeader("odmd-eee-id")
        if (entityId == null || entityId == "") {
            throw InsufficientAuthenticationException("No entity id present in odmd-eee-id")
        }

        val token = PreAuthenticatedAuthenticationToken(entityId, "", listOf(SimpleGrantedAuthority("ROLE_USER")))
        return authenticationManager.authenticate(token)
    }

    override fun successfulAuthentication(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        chain: FilterChain,
        authResult: Authentication?
    ) {
        SecurityContextHolder.getContext().authentication = authResult
        chain.doFilter(request, response)
    }
}
