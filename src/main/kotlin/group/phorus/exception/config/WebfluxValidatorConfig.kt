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
 * Needed to make the "@Validated" annotation work, used to add support for adding @Valid to collections
 * directly inside a RestController.
 */
@AutoConfiguration(before = [ValidationAutoConfiguration::class])
class WebfluxValidatorConfig {
    @Primary
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    fun defaultValidator(): LocalValidatorFactoryBean {
        val factoryBean = CustomLocalValidatorFactoryBean()
        factoryBean.messageInterpolator = MessageInterpolatorFactory().getObject()
        return factoryBean
    }
}

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
