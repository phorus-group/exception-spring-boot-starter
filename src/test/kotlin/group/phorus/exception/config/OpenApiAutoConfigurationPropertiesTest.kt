package group.phorus.exception.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.customizers.ParameterCustomizer
import org.springdoc.core.customizers.PropertyCustomizer
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Confirms that the `group.phorus.exception.openapi.*` flags gate the right beans without
 * unloading the rest of the autoconfig:
 *
 *  - defaults register every bean.
 *  - `openapi.enabled = false` skips the whole [OpenApiAutoConfiguration] class.
 *  - `openapi.x-validations.enabled = false` keeps `apiErrorSchemaAndResponses` and skips the
 *    four x-validations + group-aware beans.
 */
class OpenApiAutoConfigurationPropertiesTest {

    private val contextRunner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration::class.java))

    @Test
    fun `defaults register every customizer`() {
        contextRunner.run { context ->
            assertThat(context).hasBean("apiErrorSchemaAndResponses")
            assertThat(context).hasBean("validationsPropertyCustomizer")
            assertThat(context).hasBean("validationsParameterCustomizer")
            assertThat(context).hasBean("groupAwareOperationCustomizer")
            assertThat(context).hasBean("groupedSchemaCustomizer")
        }
    }

    @Test
    fun `openapi enabled false skips the whole autoconfig`() {
        contextRunner
            .withPropertyValues("group.phorus.exception.openapi.enabled=false")
            .run { context ->
                assertThat(context).doesNotHaveBean(OpenApiAutoConfiguration::class.java)
                assertThat(context).doesNotHaveBean(OpenApiCustomizer::class.java)
                assertThat(context).doesNotHaveBean(PropertyCustomizer::class.java)
                assertThat(context).doesNotHaveBean(ParameterCustomizer::class.java)
                assertThat(context).doesNotHaveBean(OperationCustomizer::class.java)
            }
    }

    @Test
    fun `x-validations enabled false keeps apiError customizer and drops the rest`() {
        contextRunner
            .withPropertyValues("group.phorus.exception.openapi.x-validations.enabled=false")
            .run { context ->
                assertThat(context).hasBean("apiErrorSchemaAndResponses")
                assertThat(context).doesNotHaveBean("validationsPropertyCustomizer")
                assertThat(context).doesNotHaveBean("validationsParameterCustomizer")
                assertThat(context).doesNotHaveBean("groupAwareOperationCustomizer")
                assertThat(context).doesNotHaveBean("groupedSchemaCustomizer")
            }
    }
}
