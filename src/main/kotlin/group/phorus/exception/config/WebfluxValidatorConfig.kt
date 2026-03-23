package group.phorus.exception.config

import org.hibernate.validator.internal.engine.DefaultClockProvider
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.validation.MessageInterpolatorFactory
import org.springframework.context.annotation.Bean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Role
import org.springframework.core.*
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import jakarta.validation.ClockProvider
import jakarta.validation.ParameterNameProvider
import kotlin.reflect.jvm.kotlinFunction

/**
 * Autoconfiguration that replaces the default [LocalValidatorFactoryBean] with a
 * [CustomLocalValidatorFactoryBean] to support `@Validated` on controller method
 * parameters, including `@Valid` on collection elements inside `@RestController` methods.
 *
 * Loads before [ValidationAutoConfiguration] so that the custom validator takes precedence.
 */
@AutoConfiguration(before = [ValidationAutoConfiguration::class])
class WebfluxValidatorConfig {
    /**
     * Registers a primary [LocalValidatorFactoryBean] that uses Kotlin-aware parameter
     * name discovery and a custom message interpolator.
     */
    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    fun defaultValidator(): LocalValidatorFactoryBean {
        val factoryBean = CustomLocalValidatorFactoryBean()
        factoryBean.messageInterpolator = MessageInterpolatorFactory().getObject()
        return factoryBean
    }
}

/**
 * Extended [LocalValidatorFactoryBean] that configures Kotlin-aware parameter name
 * discovery (including suspend functions) and uses Hibernate Validator's default clock provider.
 */
class CustomLocalValidatorFactoryBean : LocalValidatorFactoryBean() {
    override fun getClockProvider(): ClockProvider = DefaultClockProvider.INSTANCE
    override fun postProcessConfiguration(configuration: jakarta.validation.Configuration<*>) {
        super.postProcessConfiguration(configuration)

        val discoverer = PrioritizedParameterNameDiscoverer()
        discoverer.addDiscoverer(SuspendAwareKotlinParameterNameDiscoverer())
        discoverer.addDiscoverer(StandardReflectionParameterNameDiscoverer())

        val defaultProvider = configuration.defaultParameterNameProvider
        configuration.parameterNameProvider(object : ParameterNameProvider {
            override fun getParameterNames(constructor: Constructor<*>): List<String> {
                val paramNames = discoverer.getParameterNames(constructor)
                return paramNames?.filterNotNull() ?: defaultProvider.getParameterNames(constructor)
            }

            override fun getParameterNames(method: Method): List<String> {
                val paramNames = discoverer.getParameterNames(method)
                return paramNames?.filterNotNull() ?: defaultProvider.getParameterNames(method)
            }
        })
    }
}

/**
 * [ParameterNameDiscoverer] that delegates to [KotlinReflectionParameterNameDiscoverer]
 * and appends an extra empty-string entry for Kotlin suspend functions.
 *
 * Suspend functions compile to a method with an additional `Continuation` parameter.
 * The standard Kotlin discoverer does not account for it, which causes parameter name
 * arrays to be shorter than the actual method parameter list. This discoverer detects
 * suspend functions and pads the array so that validation parameter names align correctly.
 */
class SuspendAwareKotlinParameterNameDiscoverer : ParameterNameDiscoverer {

    private val defaultProvider = KotlinReflectionParameterNameDiscoverer()

    override fun getParameterNames(constructor: Constructor<*>): Array<String?>? =
        defaultProvider.getParameterNames(constructor)

    override fun getParameterNames(method: Method): Array<String?>? {
        val defaultNames = defaultProvider.getParameterNames(method) ?: return null
        val function = method.kotlinFunction
        return if (function != null && function.isSuspend) {
            defaultNames + ""
        } else defaultNames
    }
}
