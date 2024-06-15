package dev.odmd.platform.springcdk.ui

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class UiControllerAdvice {
    /**
     * Spring 5 -> 6 migration led to breaking changes from the Thymeleaf 2.x -> 3.1 changes
     * #httpServletRequest (thymeleaf 2.x) -> #request (3.0) -> removed for security (3.1)
     *
     * Injecting this back into the view as a temporary measure. Reference it now as `httpServletRequest` instead of
     * `#httpServletRequest`.
     */
    @ModelAttribute
    fun request(): HttpServletRequest? {
        return (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
    }
}

